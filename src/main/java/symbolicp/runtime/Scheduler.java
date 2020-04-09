package symbolicp.runtime;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;
import symbolicp.vs.MachineRefVS;
import symbolicp.vs.PrimVS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Scheduler {
    private final EventVS.Ops eventOps;
    private final PrimVS.Ops<BaseMachine> machineOps = new PrimVS.Ops<>();

    final Map<MachineTag, List<BaseMachine>> machines;
    final Map<MachineTag, PrimVS<Integer>> machineCounters;

    public Scheduler(EventVS.Ops eventOps, MachineTag... machineTags) {
        this.eventOps = eventOps;
        this.machines = new HashMap<>();
        this.machineCounters = new HashMap<>();

        for (MachineTag tag : machineTags) {
            this.machines.put(tag, new ArrayList<>());
            this.machineCounters.put(tag, new PrimVS<>(0));
        }
    }

    // minChoice is inclusive, maxChoice is exclusive
    public void addNondetChoices(int minChoice, int maxChoice, Bdd cond, Map<Integer, Bdd> dest) {
        if (maxChoice <= minChoice) {
            return;
        }

        if (minChoice + 1 == maxChoice) {
           dest.put(minChoice, cond);
           return;
        }

        int mid = minChoice + (maxChoice - minChoice) / 2;

        Bdd rightVar = Bdd.newVar();
        Bdd leftCond = cond.and(rightVar.not());
        Bdd rightCond = cond.and(rightVar);

        addNondetChoices(minChoice, mid, leftCond, dest);
        addNondetChoices(mid, maxChoice, rightCond, dest);
    }

    public Map<Integer, Bdd> getNondetChoice(int choices) {
        Map<Integer, Bdd> results = new HashMap<>();
        addNondetChoices(0, choices, Bdd.constTrue(), results);
        return results;
    }

    public void step() {
        List<MachineTag> candidateTags = new ArrayList<>();
        List<Integer> candidateIds  = new ArrayList<>();
        for (Map.Entry<MachineTag, List<BaseMachine>> entry : machines.entrySet()) {
            List<BaseMachine> machinesForTag = entry.getValue();
            for (int i = 0; i < machinesForTag.size(); i++) {
                BaseMachine machine = machinesForTag.get(i);
                if (!machine.effectQueue.isEmpty()) {
                    candidateTags.add(entry.getKey());
                    candidateIds.add(i);
                }
            }
        }

        Map<Integer, Bdd> candidateGuards = getNondetChoice(candidateTags.size());
        for (Map.Entry<Integer, Bdd> entry : candidateGuards.entrySet()) {
            MachineTag tag = candidateTags.get(entry.getKey());
            int id = candidateIds.get(entry.getKey());

            BaseMachine machine = machines.get(tag).get(id);
            Bdd guard = entry.getValue();
            List<EffectQueue.Effect> symbolicEffect = machine.effectQueue.dequeueEffect(guard);
            for (EffectQueue.Effect effect : symbolicEffect) {
                performEffect(effect);
            }
        }
    }

    public MachineRefVS allocateMachineId(Bdd pc, MachineTag tag, Function<Integer, BaseMachine> constructor) {
        final PrimVS.Ops<Integer> intOps = new PrimVS.Ops<>();
        final PrimVS.Ops<MachineTag> tagOps = new PrimVS.Ops<>();

        PrimVS<Integer> guardedCount = intOps.guard(machineCounters.get(tag), pc);
        guardedCount = guardedCount.map(i -> i + 1);

        List<BaseMachine> machineList = machines.get(tag);
        assert guardedCount.guardedValues.keySet().stream().allMatch(i -> i <= machineList.size());
        if (guardedCount.guardedValues.containsKey(machineList.size())) {
            machineList.add(constructor.apply(machineList.size()));
        }

        PrimVS<Integer> mergedCount = intOps.merge2(guardedCount, intOps.guard(machineCounters.get(tag), pc.not()));
        machineCounters.put(tag, mergedCount);

        return new MachineRefVS(tagOps.guard(new PrimVS<>(tag), pc), guardedCount);
    }

    private void performEffect(EffectQueue.Effect effect) {
        for (Map.Entry<MachineTag, Bdd> tagEntry : effect.target.tag.guardedValues.entrySet()) {
            for (Map.Entry<Integer, Bdd> idEntry : effect.target.id.guardedValues.entrySet()) {
                Bdd pc = tagEntry.getValue().and(idEntry.getValue());
                if (!pc.isConstFalse()) {
                    BaseMachine target = machines.get(tagEntry.getKey()).get(idEntry.getKey());
                    if (effect instanceof EffectQueue.SendEffect) {
                        EventVS event = ((EffectQueue.SendEffect) effect).event;
                        target.processEventToCompletion(pc, eventOps.guard(event, pc));
                    }
                    else if (effect instanceof EffectQueue.InitEffect) {
                        target.start(pc);
                    }
                    else {
                        throw new NotImplementedException();
                    }
                }
            }
        }
    }
}
