package symbolicp.runtime;

import com.sun.tools.javac.comp.Check;
import symbolicp.bdd.Checks;
import symbolicp.util.NotImplementedException;
import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.*;
import java.util.function.Function;

public class Scheduler {
    /** eventOps are value summary (VS) ops;
     * every type of VS has its own ops type created at runtime that represent the ops that can be done on them
     * events have payloads whose types are dynamically determined (can have different event types and payload types)
     * so we need to make an ops for event data structure
     */

    public static void debug(String str) {
        System.err.println("[SCHEDULER]: " + str);
    }

    final Map<MachineTag, List<BaseMachine>> machines;

    /** Refs to machines are represented by integer IDs
     * for a given Machine declaration in the P file, each machine type
     * has its own space of ids
     * MachineTag is PType of machine
     * machineCounters are used to get the next ID
     * each tag's counter gives how many of that instances tag there are */
    final Map<MachineTag, PrimVS<Integer>> machineCounters;

    private int step_count = 0;

    public Scheduler(MachineTag... machineTags) {
        this.machines = new HashMap<>();
        this.machineCounters = new HashMap<>();

        for (MachineTag tag : machineTags) {
            this.machines.put(tag, new ArrayList<>());
            this.machineCounters.put(tag, new PrimVS<>(0));
        }
    }

    /** All events and init effects, before being sent to target machine,
     * must live in another event's sender queue (because receiver queue is only represented implicitly
     * by other machines' sender queues)
     * but can't do this for the very first machine
     */
    public void startWith(MachineTag tag, BaseMachine machine) {
        debug("Start with tag " + tag + ", machine type" + machine.getClass());
        for (PrimVS<Integer> machineCounter : machineCounters.values()) {
            if (machineCounter.guardedValues.size() != 1 || !machineCounter.guardedValues.containsKey(0)) {
                throw new RuntimeException("You cannot start the scheduler after it already contains machines");
            }
        }

        machineCounters.put(tag, new PrimVS<>(1));
        machines.get(tag).add(machine);

        performEffect(
            new EffectQueue.InitEffect(
                    Bdd.constTrue(),
                    new MachineRefVS(new PrimVS<>(tag), new PrimVS<>(0))
            )
        );
    }


    private OptionalVS<PrimVS<Integer>> getNondetChoice(List<Bdd> candidateConds) {

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

        assert(Checks.sameUniverse(noneEnabledCond, new PrimVS<Integer>().merge(results).getUniverse()));
        return new OptionalVS<>(new PrimVS<Integer>().merge(results));
    }

    public boolean step() {
        List<MachineTag> candidateTags = new ArrayList<>();
        List<Integer> candidateIds = new ArrayList<>();
        List<Bdd> candidateConds = new ArrayList<>();
        step_count++;

        for (Map.Entry<MachineTag, List<BaseMachine>> entry : machines.entrySet()) {
            List<BaseMachine> machinesForTag = entry.getValue();
            for (int i = 0; i < machinesForTag.size(); i++) {
                BaseMachine machine = machinesForTag.get(i);
                debug("machine with tag " + entry.getValue() + ", machine type" + machine.getClass());
                if (!machine.effectQueue.isEmpty()) {
                    candidateTags.add(entry.getKey());
                    candidateIds.add(i);
                    candidateConds.add(machine.effectQueue.enabledCond());
                }
            }
        }

        if (candidateTags.isEmpty()) {
            RuntimeLogger.log(String.format("Execution finished in %d steps", step_count));
            return true;
        }

        OptionalVS<PrimVS<Integer>> candidateGuards = getNondetChoice(candidateConds);
        for (Map.Entry<Integer, Bdd> entry : candidateGuards.unwrapOrThrow().guardedValues.entrySet()) {
            MachineTag tag = candidateTags.get(entry.getKey());
            int id = candidateIds.get(entry.getKey());

            BaseMachine machine = machines.get(tag).get(id);
            Bdd guard = entry.getValue();
            List<EffectQueue.Effect> symbolicEffect = machine.effectQueue.dequeueEntry(guard);
            for (EffectQueue.Effect effect : symbolicEffect) {
                performEffect(effect);
            }
        }

        return false;
    }

    public MachineRefVS allocateMachineId(Bdd pc, MachineTag tag, Function<Integer, BaseMachine> constructor) {
        PrimVS<Integer> guardedCount = machineCounters.get(tag).guard(pc);
        guardedCount = guardedCount.apply(i -> i + 1);

        List<BaseMachine> machineList = machines.get(tag);
        // TODO: potential off by one error fixed in below two lines. Review required
        assert guardedCount.guardedValues.keySet().stream().allMatch(i -> i <= machineList.size() + 1);

        if (guardedCount.guardedValues.containsKey(machineList.size() + 1)) {
            machineList.add(constructor.apply(machineList.size()));
        }

        PrimVS<Integer> mergedCount = guardedCount.merge(machineCounters.get(tag).guard(pc.not()));
        machineCounters.put(tag, mergedCount);
        return new MachineRefVS(new PrimVS<>(tag).guard(pc), guardedCount.apply(i -> i - 1));
    }

    private void performEffect(EffectQueue.Effect effect) {
        for (Map.Entry<MachineTag, Bdd> tagEntry : effect.target.tag.guardedValues.entrySet()) {
            for (Map.Entry<Integer, Bdd> idEntry : effect.target.id.guardedValues.entrySet()) {
                Bdd pc = tagEntry.getValue().and(idEntry.getValue());
                if (!pc.isConstFalse()) {
                    BaseMachine target = machines.get(tagEntry.getKey()).get(idEntry.getKey());
                    if (effect instanceof EffectQueue.SendEffect) {
                        UnionVS<EventTag> event = ((EffectQueue.SendEffect) effect).event;
                        target.processEventToCompletion(pc, event.guard(pc));
                    } else if (effect instanceof EffectQueue.InitEffect) {
                        target.start(pc, ((EffectQueue.InitEffect) effect).payload);
                    } else {
                        throw new NotImplementedException();
                    }
                }
            }
        }
    }

    public void logState() {
        for (Map.Entry<MachineTag, List<BaseMachine>> entry : machines.entrySet()) {
            List<BaseMachine> machinesWithTag = entry.getValue();
            for (int i = 0; i < machinesWithTag.size(); i++) {
                RuntimeLogger.log(
                        String.format("Machine (tag: %s, index: %d): %s", entry.getKey(), i, machinesWithTag.get(i))
                );
            }
        }
    }

    public void disableLogging() {
        RuntimeLogger.disableInfo();
    }

    public void enableLogging() {
        RuntimeLogger.enableInfo();
    }
}
