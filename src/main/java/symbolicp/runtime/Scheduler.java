package symbolicp.runtime;

import symbolicp.util.Checks;
import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.*;
import java.util.function.Function;

public class Scheduler {

    public static Schedule schedule;
    public static Bdd universe;

    final List<Machine> machines;
    final Map<Class<? extends Machine>, PrimVS<Integer>> machineCounters;

    private int step_count = 0;
    private boolean done = false;

    public Scheduler(Machine... machines) {
        universe = Bdd.constTrue();
        schedule = new Schedule();
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
        assert(universe.isConstTrue());
        for (PrimVS<Integer> machineCounter : machineCounters.values()) {
            if (machineCounter.getGuardedValues().size() != 1 || !machineCounter.hasValue(0)) {
                throw new RuntimeException("You cannot start the scheduler after it already contains machines");
            }
        }

        machineCounters.put(machine.getClass(), new PrimVS<>(1));
        machines.add(machine);

        performEffect(
            new Event(
                    EventName.Init.instance,
                    new PrimVS<>(machine),
                    null
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
        schedule.step();
        if (done) return true;

        for (Machine machine : machines) {
            if (!machine.effectQueue.isEmpty()) {
                candidateMachines.add(machine);
                candidateConds.add(machine.effectQueue.enabledCond(Event::canRun).getGuard(true));
            }
        }

        if (candidateMachines.isEmpty()) {
            ScheduleLogger.finished(step_count);
            done = true;
            return true;
        }

        PrimVS<Integer> candidateGuards = getNondetChoice(candidateConds);

        for (GuardedValue<Integer> entry : candidateGuards.getGuardedValues()) {
            Machine machine = candidateMachines.get(entry.value);
            Bdd guard = entry.guard;
            universe = guard;
            Event effect = machine.effectQueue.dequeueEntry(guard);
            ScheduleLogger.schedule(step_count, effect, machines);
            schedule.addToSchedule(guard, effect, machines);
            PrimVS<State> oldState = machine.getState();
            assert(effect.getUniverse().implies(guard).isConstTrue());
            performEffect(effect);
            // After the effect is performed, the machine state should be the same under all other path conditions
            assert(Checks.equalUnder(oldState, machine.getState(), machine.getState().guard(guard.not()).getUniverse()));
        }
        universe = Bdd.constTrue();
        return false;
    }

    public PrimVS<Machine> allocateMachine(Bdd pc, Class<? extends Machine> machineType,
                                           Function<Integer, ? extends Machine> constructor) {
        if (!machineCounters.containsKey(machineType)) {
            machineCounters.put(machineType, new PrimVS<>(0));
        }
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

    private void performEffect(Event event) {
        for (GuardedValue<Machine> target : event.getMachine().getGuardedValues()) {
            target.value.processEventToCompletion(target.guard, event.guard(target.guard));
        }
    }

    public void logState() {
        for (Machine machine : machines) {
            ScheduleLogger.machineState(machine);
        }
    }

    public void disableLogging() {
        ScheduleLogger.disableInfo();
    }

    public void enableLogging() {
        ScheduleLogger.enableInfo();
    }
}
