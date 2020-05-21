package symbolicp.runtime;

import symbolicp.util.NotImplementedException;
import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.*;
import java.util.function.Function;

public class Scheduler {
    private final UnionVS.Ops<EventTag> eventOps;
    private final PrimVS.Ops<BaseMachine> machineOps = new PrimVS.Ops<>();

    final Map<MachineTag, List<BaseMachine>> machines;

    final Map<MachineTag, PrimVS<Integer>> machineCounters;

    private int step_count = 0;

    public Scheduler(UnionVS.Ops<EventTag> eventOps, MachineTag... machineTags) {
        this.eventOps = eventOps;
        this.machines = new HashMap<>();
        this.machineCounters = new HashMap<>();

        for (MachineTag tag : machineTags) {
            this.machines.put(tag, new ArrayList<>());
            this.machineCounters.put(tag, new PrimVS<>(0));
        }
    }

    public void startWith(MachineTag tag, BaseMachine machine) {
        for (PrimVS<Integer> machineCounter : machineCounters.values()) {
            if (machineCounter.guardedValues.size() != 1 || !machineCounter.guardedValues.containsKey(0)) {
                throw new RuntimeException("You cannot start the scheduler after it already contains machines");
            }
        }

        machineCounters.put(tag, new PrimVS<>(1));
        machines.get(tag).add(machine);

        performEffect(
            new EffectQueue.InitEffect(
                    eventOps,
                    Bdd.constTrue(),
                    new MachineRefVS(new PrimVS<>(tag), new PrimVS<>(0))
            )
        );
    }


    private OptionalVS<PrimVS<Integer>> getNondetChoice(List<Bdd> candidateConds) {
        final PrimVS.Ops<Integer> intOps = new PrimVS.Ops<>();

        List<PrimVS<Integer>> results = new ArrayList<>();

        Bdd residualPc = Bdd.constTrue();
        for (int i = 0; i < candidateConds.size(); i++) {
            Bdd enabledCond = candidateConds.get(i);
            Bdd choiceCond = Bdd.newVar().and(enabledCond);

            Bdd returnPc = residualPc.and(choiceCond);
            results.add(intOps.guard(new PrimVS<>(i), returnPc));

            residualPc = residualPc.and(choiceCond.not());
        }

        for (int i = 0; i < candidateConds.size(); i++) {
            Bdd enabledCond = candidateConds.get(i);

            Bdd returnPc = residualPc.and(enabledCond);
            results.add(intOps.guard(new PrimVS<>(i), returnPc));

            residualPc = residualPc.and(enabledCond.not());
        }

        final Bdd noneEnabledCond = residualPc;
        PrimVS<Boolean> isPresent = BoolUtils.fromTrueGuard(noneEnabledCond.not());

        return new OptionalVS<>(isPresent, intOps.merge(results));
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
        for (Map.Entry<Integer, Bdd> entry : candidateGuards.item.guardedValues.entrySet()) {
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
        final PrimVS.Ops<Integer> intOps = new PrimVS.Ops<>();
        final PrimVS.Ops<MachineTag> tagOps = new PrimVS.Ops<>();

        PrimVS<Integer> guardedCount = intOps.guard(machineCounters.get(tag), pc);
        guardedCount = guardedCount.map(i -> i + 1);

        List<BaseMachine> machineList = machines.get(tag);
        // TODO: potential off by one error fixed in below two lines. Review required
        assert guardedCount.guardedValues.keySet().stream().allMatch(i -> i <= machineList.size() + 1);

        if (guardedCount.guardedValues.containsKey(machineList.size() + 1)) {
            machineList.add(constructor.apply(machineList.size()));
        }

        PrimVS<Integer> mergedCount = intOps.merge2(guardedCount, intOps.guard(machineCounters.get(tag), pc.not()));
        machineCounters.put(tag, mergedCount);
        return new MachineRefVS(tagOps.guard(new PrimVS<>(tag), pc), guardedCount.map(i -> i - 1));
    }

    private void performEffect(EffectQueue.Effect effect) {
        for (Map.Entry<MachineTag, Bdd> tagEntry : effect.target.tag.guardedValues.entrySet()) {
            for (Map.Entry<Integer, Bdd> idEntry : effect.target.id.guardedValues.entrySet()) {
                Bdd pc = tagEntry.getValue().and(idEntry.getValue());
                if (!pc.isConstFalse()) {
                    BaseMachine target = machines.get(tagEntry.getKey()).get(idEntry.getKey());
                    if (effect instanceof EffectQueue.SendEffect) {
                        UnionVS<EventTag> event = ((EffectQueue.SendEffect) effect).event;
                        target.processEventToCompletion(pc, eventOps.guard(event, pc));
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
