package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.ArrayList;
import java.util.List;

public class DporSchedule extends Schedule {

    @Override
    public VectorClockManager getNewVectorClockManager() {
        return new VectorClockManager(true);
    }

    public class DporChoice extends Schedule.Choice {
        private boolean frozen = false;

        @Override
        public void clear() {
            super.clear();
            frozen = false;
        }

        public void freeze() { frozen = true; }

        public boolean isFrozen() { return frozen; }

        @Override
        public void addSenderChoice(PrimVS<Machine> choice) {
            senderChoice = choice;
            // don't try to add the event since it may not be "current"
            // can look the event up in the full backtrack set
        }
    }

    List<DporChoice> dporBacktrackChoice = new ArrayList<>();

    @Override
    public void addBacktrackSender(PrimVS<Machine> choice, int depth) {
        super.addBacktrackSender(choice, depth);
        if (depth >= dporBacktrackChoice.size()) {
            dporBacktrackChoice.add(new DporChoice());
        }
        // note: don't add the sender yet, since we don't add backtracking choice up-front with TransDPOR!
    }

    @Override
    public void addBacktrackBool(PrimVS<Boolean> choice, int depth) {
        super.addBacktrackBool(choice, depth);
        if (depth >= dporBacktrackChoice.size()) {
            dporBacktrackChoice.add(new DporChoice());
        }
        dporBacktrackChoice.get(depth).addBoolChoice(choice);
    }

    @Override
    public void addBacktrackInt(PrimVS<Integer> choice, int depth) {
        super.addBacktrackInt(choice, depth);
        if (depth >= dporBacktrackChoice.size()) {
            dporBacktrackChoice.add(new DporChoice());
        }
        dporBacktrackChoice.get(depth).addIntChoice(choice);
    }

    public void addBacktrackElement(PrimVS<ValueSummary> choice, int depth) {
        super.addBacktrackElement(choice, depth);
        if (depth >= dporBacktrackChoice.size()) {
            dporBacktrackChoice.add(new DporChoice());
        }
        dporBacktrackChoice.get(depth).addElementChoice(choice);
    }

    @Override
    public int getNumBacktracks() {
        int count = 0;
        for (Choice backtrack : dporBacktrackChoice) {
            if (!backtrack.isEmpty()) count++;
        }
        return count;
    }

    public void compare(PrimVS<Machine> senders) {
        ScheduleLogger.log("compare!");
        Event pending = new Event();
        List<Event> toMerge = new ArrayList<>();
        for (GuardedValue<Machine> guardedValue : senders.getGuardedValues()) {
            EffectCollection effects = guardedValue.value.sendEffects;
            if (!effects.isEmpty())
                toMerge.add(effects.peek(guardedValue.guard.and(effects.enabledCond(Event::canRun).getGuard(true))));
        }
        pending = pending.merge(toMerge);
        for (GuardedValue<Machine> pendingTgt : pending.getMachine().getGuardedValues()) {
            int size = size();
            boolean found = false;
            for (int i = size - 1; i >= 0 && !found; i--) {
                Event event = getRepeatChoice(i).eventChosen;
                event = event.guard(event.isInit().getGuard(false));
                PrimVS<Machine> target = event.getMachine();
                for (GuardedValue<Machine> tgt : target.guard(pendingTgt.guard).getGuardedValues()) {
                    if (tgt.value.equals(pendingTgt.value)) {
                        if (!this.dporBacktrackChoice.get(i).isFrozen()) {
                            ScheduleLogger.log("adding backtrack to " + i);
                            Choice choices = super.getBacktrackChoice(i).guard(pendingTgt.guard);
                            PrimVS<Machine> backtrack = getPrePending(pending.guard(pendingTgt.guard), choices);
                            if (!backtrack.isEmptyVS()) {
                                dporBacktrackChoice.get(i).addSenderChoice(backtrack);
                                dporBacktrackChoice.get(i).freeze();
                                found = true;
                            } else {
                                ScheduleLogger.log("backtrack empty at " + i);
                                ScheduleLogger.log("other event is " + event.guard(pendingTgt.guard).toString());
                                ScheduleLogger.log("other target is " + tgt.value.toString());
                                ScheduleLogger.log("pending events are " + pending.guard(pendingTgt.guard).toString());
                                ScheduleLogger.log("pending target is " + pendingTgt.value.toString());
                                ScheduleLogger.log("sender choices are " + choices.senderChoice.toString());
                                ScheduleLogger.log("full sender choices are " + getSenderChoice(i).toString());
                                ScheduleLogger.log("full sender choice events are " + getFullChoice(i).eventChosen.toString());
                                ScheduleLogger.log("backtrack is " + backtrack.toString());
                            }
                        } else {
                            ScheduleLogger.log("frozen at " + i);
                            if (this.dporBacktrackChoice.get(i).isEmpty()) {
                                throw new RuntimeException();
                            }
                        }
                    }
                }
            }
        }
    }

    PrimVS<Machine> getPrePending(Event pending, Choice choices) {
        VectorClockVS pendingClock = pending.getVectorClock();
        VectorClockVS other = choices.eventChosen.getVectorClock();
        PrimVS<Integer> cmp = other.cmp(pendingClock);
        // either it IS the pending event
        // or it happens-before the pending event
        // (double-check if this is necessary: AND it was some later-scheduled thing?)
        PrimVS<Boolean> before = IntUtils.lessThan(cmp, 1); // true when other is or happens-before pending
        Bdd happensBeforeCond = before.getGuard(true);
        if (!happensBeforeCond.isConstFalse()) {
            return choices.senderChoice.guard(happensBeforeCond);
        }
        return new PrimVS<>();
    }

    @Override
    public Choice getBacktrackChoice(int depth) {
        return dporBacktrackChoice.get(depth);
    }

    @Override
    public void clearBacktrack(int depth) {
        super.getBacktrackChoice(depth).guard(getBacktrackChoice(depth).getUniverse().not());
        dporBacktrackChoice.get(depth).clear();
    }
}
