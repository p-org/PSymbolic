package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.GuardedValue;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummary;

import java.util.*;

public class Schedule {
    List<PrimVS<EffectQueue.Effect>> schedule = new ArrayList<>();
    List<Map<Machine, PrimVS<State>>> machines = new ArrayList<>();
    List<List<PrimVS>> withinStep = new ArrayList<>();
    List<List<PrimVS>> ranEvent = new ArrayList<>();
    int size = 0;

    public Schedule() {
        withinStep.add(new ArrayList<>());
        ranEvent.add(new ArrayList<>());
    }

    public void step() {
        schedule.add(new PrimVS<>());
        this.machines.add(new HashMap<>());
        this.withinStep.add(new ArrayList<>());
        this.ranEvent.add(new ArrayList<>());
        size++;
    }

    public void addToSchedule(Bdd pc, List<EffectQueue.Effect> effects, List<Machine> machines) {
        for (EffectQueue.Effect effect : effects) {
            this.schedule.set(size - 1, schedule.get(schedule.size() - 1).merge((new PrimVS<>(effect)).guard(effect.getCond())));
        }
        for (Machine m : machines) {
            this.machines.get(size - 1).put(m, this.machines.get(size - 1).getOrDefault(m, new PrimVS<>()).update(pc, m.getState()));
        }
    }

    public void withinStep(PrimVS<State> state) {
        this.withinStep.get(size).add(state);
    }
    public void runEvent(PrimVS<Event> event) {
        this.ranEvent.get(size).add(event);
    }

    public GuardedValue<String> getSingleSubStep(Bdd pc, List<PrimVS> subSteps) {
        String substep = "";
        Bdd currentPc = pc;
        if (subSteps.size() > 0) {
            boolean first = true;
            for (PrimVS step : subSteps) {
                List<GuardedValue> steps = step.guard(currentPc).getGuardedValues();
                if (steps.size() > 0) {
                    if (!first) {
                        substep += "->";
                    } else {
                        first = false;
                        substep += "    ";
                    }
                    substep += steps.get(0).value;
                    substep += "(" + steps.size() + ")";
                    currentPc = currentPc.and(steps.get(0).guard);
                }
            }
            substep += System.lineSeparator();
        }
        return new GuardedValue<>(substep, currentPc);
    }

    public String singleScheduleToString(Bdd pc) {
        String scheduleString = "";
        Bdd currentPc = pc;

        for (int i = 0; i <= size; i++) {
            scheduleString += ("Substeps at " + (i - 1) + ": ");
            List<PrimVS> subSteps = withinStep.get(i);
            GuardedValue<String> withinStepResult = getSingleSubStep(currentPc, subSteps);
            scheduleString += withinStepResult.value;
            currentPc = withinStepResult.guard;

            System.out.println("Event ran at " + (i - 1) + ": ");
            List<PrimVS> ran = ranEvent.get(i);
            GuardedValue<String> ranEventResult = getSingleSubStep(currentPc, ran);
            scheduleString += withinStepResult.value;
            currentPc = withinStepResult.guard;

            if (i == size) break;
            scheduleString += "State at " + i + ": ";
            for (Map.Entry<Machine, PrimVS<State>> entry : this.machines.get(i).entrySet()) {
                List<GuardedValue<State>> statesOnPath = entry.getValue().guard(currentPc).getGuardedValues();
                if (statesOnPath.size() > 0) {
                    GuardedValue<State> guardedValue = statesOnPath.get(0);
                    scheduleString += "    " + entry.getKey() + "@" + guardedValue.value.name;
                    currentPc = currentPc.and(guardedValue.guard);
                }
            }
            scheduleString += "Schedule step " + i + ": ";
            List<GuardedValue<EffectQueue.Effect>> effects = schedule.get(i).guard(currentPc).getGuardedValues();
            if (effects.size() > 0) {
                GuardedValue<EffectQueue.Effect> guardedValue = effects.get(0);
                scheduleString +=  "    " + guardedValue.value;
                currentPc = currentPc.and(guardedValue.guard);
            }
        }

        return scheduleString;
    }
}
