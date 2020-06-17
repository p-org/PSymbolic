package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.GuardedValue;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummary;

import java.util.*;

public class Schedule {
    List<Event> schedule = new ArrayList<>();
    List<Map<Machine, PrimVS<State>>> machines = new ArrayList<>();
    List<List<PrimVS<State>>> withinStep = new ArrayList<>();
    List<List<Event>> ranEvent = new ArrayList<>();
    int size = 0;

    public Schedule() {
        withinStep.add(new ArrayList<>());
        ranEvent.add(new ArrayList<>());
    }

    public void step() {
        schedule.add(new Event());
        this.machines.add(new HashMap<>());
        this.withinStep.add(new ArrayList<>());
        this.ranEvent.add(new ArrayList<>());
        size++;
    }

    public void addToSchedule(Bdd pc, Event effect, List<Machine> machines) {
        this.schedule.set(size - 1, schedule.get(size - 1).merge(effect));
        for (Machine m : machines) {
            this.machines.get(size - 1).put(m, this.machines.get(size - 1).getOrDefault(m, new PrimVS<>()).update(pc, m.getState()));
        }
    }

    public void withinStep(PrimVS<State> state) {
        this.withinStep.get(size).add(state);
    }
    public void runEvent(Event event) {
        this.ranEvent.get(size).add(event);
    }

    public GuardedValue<String> getSingleSubStep(Bdd pc, List<PrimVS<State>> subSteps) {
        String substep = "";
        Bdd currentPc = pc;
        if (subSteps.size() > 0) {
            boolean first = true;
            for (PrimVS<State> step : subSteps) {
                List<GuardedValue<State>> steps = step.guard(currentPc).getGuardedValues();
                if (steps.size() > 0) {
                    if (!first) {
                        substep += "->";
                    } else {
                        first = false;
                    }
                    substep += steps.get(0).value;
                    substep += "(" + steps.size() + ")";
                    currentPc = currentPc.and(steps.get(0).guard);
                }
            }
        }
        substep += System.lineSeparator();
        return new GuardedValue<>(substep, currentPc);
    }

    public String singleScheduleToString(Bdd pc) {
        String scheduleString = "";
        Bdd currentPc = pc;

        for (int i = 0; i <= size; i++) {
            scheduleString += "Substeps at " + (i - 1) + ": ";
            List<PrimVS<State>> subSteps = withinStep.get(i);
            GuardedValue<String> withinStepResult = getSingleSubStep(currentPc, subSteps);
            scheduleString += withinStepResult.value;
            currentPc = withinStepResult.guard;

            List<Event> ran = ranEvent.get(i);
            scheduleString += "Events ran at " + (i - 1) + ": " + System.lineSeparator();
            for (Event e : ran) {
                if (!e.guard(currentPc).isEmptyVS())
                    scheduleString += e.guard(currentPc) + System.lineSeparator();
            }

            if (i == size) break;
            scheduleString += "State at " + i + ": ";
            scheduleString += System.lineSeparator();
            for (Map.Entry<Machine, PrimVS<State>> entry : this.machines.get(i).entrySet()) {
                List<GuardedValue<State>> statesOnPath = entry.getValue().guard(currentPc).getGuardedValues();
                if (statesOnPath.size() > 0) {
                    GuardedValue<State> guardedValue = statesOnPath.get(0);
                    scheduleString += "    " + entry.getKey() + "@" + guardedValue.value.name + "(" + statesOnPath.size() + ")";
                    scheduleString += System.lineSeparator();
                    currentPc = currentPc.and(guardedValue.guard);
                }
            }
            scheduleString += "Schedule step " + i + ": ";
            scheduleString += System.lineSeparator();
            Event effects = schedule.get(i).guard(currentPc);
            if (!effects.isEmptyVS()) {
                scheduleString +=  "    " + effects.toString();
                scheduleString += System.lineSeparator();
                currentPc = currentPc.and(effects.getUniverse());
            }
        }

        return scheduleString;
    }
}
