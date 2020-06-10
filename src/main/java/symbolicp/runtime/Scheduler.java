package symbolicp.runtime;

import symbolicp.bdd.Checks;
import symbolicp.util.NotImplementedException;
import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.*;
import java.util.function.Function;

public class Scheduler {

    final List<Machine> machines;
    final Map<Class<? extends Machine>, PrimVS<Integer>> machineCounters;

    private int step_count = 0;

    public Scheduler(Machine... machines) {
        this.machines = new ArrayList<>();
        this.machineCounters = new HashMap<>();

        for (Machine machine : machines) {
            this.machines.add(machine);
            if (this.machineCounters.containsKey(machine.getClass())) {
                this.machineCounters.putIfAbsent(machine.getClass(),
                        IntUtils.add(this.machineCounters.get(machine.getClass()), 1));
            }
            this.machineCounters.put(machine.getClass(), new PrimVS<>(0));
        }
    }

    /** All events and init effects, before being sent to target machine,
     * must live in another event's sender queue (because receiver queue is only represented implicitly
     * by other machines' sender queues)
     * but can't do this for the very first machine
     */
    public void startWith(Machine machine) {
        RuntimeLogger.log("Start with tag " + machine.getClass() + ", machine type" + machine.getClass());
        for (PrimVS<Integer> machineCounter : machineCounters.values()) {
            if (machineCounter.getGuardedValues().size() != 1 || !machineCounter.hasValue(0)) {
                throw new RuntimeException("You cannot start the scheduler after it already contains machines");
            }
        }

        machineCounters.put(machine.getClass(), new PrimVS<>(1));
        machines.add(machine);

        performEffect(
            new EffectQueue.InitEffect(
                    Bdd.constTrue(),
                    new PrimVS<Machine>(machine)
            )
        );
    }


    private PrimVS<Integer> getNondetChoice(List<Bdd> candidateConds) {

        List<PrimVS<Integer>> results = new ArrayList<>();

        Bdd residualPc = Bdd.constTrue();
        for (int i = 0; i < candidateConds.size(); i++) {
            Bdd enabledCond = candidateConds.get(i);
            Bdd choiceCond = Bdd.newVar().and(enabledCond);

            Bdd returnPc = residualPc.and(choiceCond);
            results.add(new PrimVS<>(i).guard(returnPc));

            residualPc = residualPc.and(choiceCond.not());
        }

        for (int i = 0; i < candidateConds.size(); i++) {
            Bdd enabledCond = candidateConds.get(i);

            Bdd returnPc = residualPc.and(enabledCond);
            results.add(new PrimVS<>(i).guard(returnPc));

            residualPc = residualPc.and(enabledCond.not());
        }

        final Bdd noneEnabledCond = residualPc;
        PrimVS<Boolean> isPresent = BoolUtils.fromTrueGuard(noneEnabledCond.not());

        assert(Checks.sameUniverse(noneEnabledCond.not(), new PrimVS<Integer>().merge(results).getUniverse()));
        return new PrimVS<Integer>().merge(results);
    }

    public boolean step() {
        List<Machine> candidateMachines = new ArrayList<>();
        List<Bdd> candidateConds = new ArrayList<>();
        step_count++;

        for (Machine machine : machines) {
            RuntimeLogger.log("Machine with name " + machine.name + "machine type" + machine.getClass());
            if (!machine.effectQueue.isEmpty()) {
                candidateMachines.add(machine);
                candidateConds.add(machine.effectQueue.enabledCond());
            }
        }

        if (candidateMachines.isEmpty()) {
            RuntimeLogger.log(String.format("Execution finished in %d steps", step_count));
            return true;
        }

        PrimVS<Integer> candidateGuards = getNondetChoice(candidateConds);
        for (GuardedValue<Integer> entry : candidateGuards.getGuardedValues()) {
            Machine machine = candidateMachines.get(entry.value);
            Bdd guard = entry.guard;
            List<EffectQueue.Effect> symbolicEffect = machine.effectQueue.dequeueEntry(guard);
            for (EffectQueue.Effect effect : symbolicEffect) {
                performEffect(effect);
            }
        }

        return false;
    }

    public PrimVS<Machine> allocateMachine(Bdd pc, Class<? extends Machine> machineType,
                                           Function<Integer, ? extends Machine> constructor) {
        PrimVS<Integer> guardedCount = machineCounters.get(machineType).guard(pc);
        guardedCount = IntUtils.add(guardedCount, 1);

        Machine newMachine;
        newMachine = constructor.apply(IntUtils.maxValue(guardedCount));

        if (!machines.contains(newMachine)) {
            machines.add(newMachine);
        }

        PrimVS<Integer> mergedCount = machineCounters.get(machineType).update(pc, guardedCount);
        machineCounters.put(machineType, mergedCount);
        return new PrimVS<>(newMachine).guard(pc);
    }

    private void performEffect(EffectQueue.Effect effect) {
        for (GuardedValue<Machine> target : effect.target.getGuardedValues()) {
            Bdd pc = target.guard;
            if (!pc.isConstFalse()) {
                if (effect instanceof EffectQueue.SendEffect) {
                    PrimVS<Event> event = ((EffectQueue.SendEffect) effect).event;
                    target.value.processEventToCompletion(pc, event.guard(pc));
                } else if (effect instanceof EffectQueue.InitEffect) {
                    target.value.start(pc, ((EffectQueue.InitEffect) effect).payload);
                } else {
                    throw new NotImplementedException();
                }
            }
        }
    }

    public void logState() {
        for (Machine machine : machines) {
            RuntimeLogger.log(String.format("Machine %s:", machine.toString()));
        }
    }

    public void disableLogging() {
        RuntimeLogger.disableInfo();
    }

    public void enableLogging() {
        RuntimeLogger.enableInfo();
    }
}
