package symbolicp.runtime;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;
import symbolicp.vs.PrimVS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scheduler {
    private final EventVS.Ops eventOps;
    private final PrimVS.Ops<BaseMachine> machineOps = new PrimVS.Ops<>();

    final Map<MachineTag, List<BaseMachine>> machines;
    final Map<MachineTag, PrimVS<BaseMachine>> machineCounters;

    public Scheduler(EventVS.Ops eventOps) {
        this.eventOps = eventOps;
        this.machines = new HashMap<>();
        this.machineCounters = new HashMap<>();
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

    private void performEffect(EffectQueue.Effect effect) {
        if (effect instanceof EffectQueue.SendEffect) {
            for (Map.Entry<MachineTag, Bdd> tagEntry : effect.target.tag.guardedValues.entrySet()) {
                for (Map.Entry<Integer, Bdd> idEntry : effect.target.id.guardedValues.entrySet()) {
                    Bdd pc = tagEntry.getValue().and(idEntry.getValue());
                    if (!pc.isConstFalse()) {
                        BaseMachine target = machines.get(tagEntry.getKey()).get(idEntry.getKey());
                        EventVS event = ((EffectQueue.SendEffect) effect).event;
                        target.processEventToCompletion(pc, eventOps.guard(event, pc));
                    }
                }
            }
        } else if (effect instanceof EffectQueue.MachineCreationEffect){
            /* Register machine to scheduler under the machine tag provided with the machine */
            for (Map.Entry<BaseMachine, Bdd> entry : ((EffectQueue.MachineCreationEffect) effect).machine.guardedValues.entrySet()) {
                Bdd pc = entry.getValue();
                BaseMachine machine = entry.getKey();
                Map<BaseMachine, Bdd> map = new HashMap<>(); map.put(machine, pc);
                PrimVS<BaseMachine> merge_segment = new PrimVS<>(map);
                if (!pc.isConstFalse()) {
                    machines.get(machine.getMachineTag()).add(machine);
                    machineCounters.put(machine.getMachineTag(),
                            machineOps.merge2(machineCounters.get(machine.getMachineTag()), merge_segment));
                }
                /* Start the machine after registration */
                machine.start(pc);
            }
        }
        else {
            throw new NotImplementedException();
        }
    }
}
