package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.nio.channels.ShutdownChannelGroupException;
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
                event = event.guard(event.getName().getGuard(EventName.Init.instance).not());
                PrimVS<Machine> target = event.getMachine();
                for (GuardedValue<Machine> tgt : target.getGuardedValues()) {
                    if (tgt.value.equals(pendingTgt.value)) {
                        Bdd cmpUniverse = pendingTgt.guard.and(tgt.guard);
                        // make sure that there isn't a happens-before relationship between the pending and the
                        // potential choice to replace:
                        PrimVS<Integer> cmp = pending.guard(cmpUniverse).getVectorClock().cmp(event.guard(cmpUniverse).getVectorClock());
                        Bdd notAfter = cmp.getGuard(1).not();
                        Event prePending = pending.guard(notAfter.and(pendingTgt.guard));
                        PrimVS<Machine> preSenders = senders.guard(notAfter.and(pendingTgt.guard));
                        if (prePending.isEmptyVS()) { continue; }
                        if (!this.dporBacktrackChoice.get(i).isFrozen()) {
                            ScheduleLogger.log("try backtrack for " + prePending.guard(pendingTgt.guard));
                            ScheduleLogger.log("try run instead of " + event.guard(tgt.guard));
                            PrimVS<Machine> backtrack = getPrePending(prePending, preSenders, i);
                            if (!backtrack.isEmptyVS()) {
                                ScheduleLogger.log("need to backtrack for " + prePending);
                                ScheduleLogger.log("run instead of " + event.guard(tgt.guard));
                                dporBacktrackChoice.get(i).addSenderChoice(backtrack);
                                dporBacktrackChoice.get(i).freeze();
                                found = true;
                            } else {
                                ScheduleLogger.log("backtrack empty at " + i);
                                ScheduleLogger.log("wanted to backtrack for " + prePending);
                                ScheduleLogger.log("wanted to run instead of " + event.guard(tgt.guard));
                                /*
                                ScheduleLogger.log("other event is " + event.guard(pendingTgt.guard).toString());
                                ScheduleLogger.log("other target is " + tgt.value.toString());
                                ScheduleLogger.log("pending events are " + pending.guard(pendingTgt.guard).toString());
                                ScheduleLogger.log("pending target is " + pendingTgt.value.toString());
                                ScheduleLogger.log("sender choices are " + choices.senderChoice.toString());
                                ScheduleLogger.log("full sender choices are " + getSenderChoice(i).toString());
                                ScheduleLogger.log("full sender choice events are " + getFullChoice(i).eventChosen.toString());
                                ScheduleLogger.log("backtrack is " + backtrack.toString());
                                */
                            }
                        }
                    }
                }
            }
        }
    }

    Bdd canSchedule(PrimVS<Machine> machine, int i) {
        Choice choices = super.getBacktrackChoice(i);
        Bdd cond = Bdd.constFalse();
        for (GuardedValue<Machine> guardedValue0 : choices.senderChoice.getGuardedValues()) {
            for (GuardedValue<Machine> guardedValue1 : machine.getGuardedValues()) {
                if (guardedValue0.value.equals(guardedValue1.value)) {
                    cond = cond.or(guardedValue0.guard);
                }
            }
        }
        return cond;
    }

    PrimVS<Machine> getPrePending(Event pending, PrimVS<Machine> pendingSenders, int i) {
        VectorClockVS pendingClock = pending.getVectorClock();
        for (int j = i; j < size(); j++) {
            // either it IS the pending event
            Choice backtrack = super.getBacktrackChoice(i);
            Bdd cond = canSchedule(pendingSenders, i);
            if (!cond.isConstFalse()) // if it is enabled at i
                return backtrack.senderChoice.guard(cond);
            // or it happens-before the pending event
            Choice previous = getRepeatChoice(j);
            VectorClockVS other = previous.eventChosen.getVectorClock();
            PrimVS<Integer> cmp = other.guard(pending.getUniverse()).cmp(pendingClock);
            PrimVS<Boolean> before = IntUtils.lessThan(cmp, 1); // true when other is or happens-before pending
            Bdd happensBeforeCond = before.getGuard(true);
            if (!happensBeforeCond.isConstFalse()) {
                PrimVS<Machine> queue = previous.senderChoice.guard(happensBeforeCond);
                Choice choices = super.getBacktrackChoice(i);
                ScheduleLogger.log("previous: " + previous.eventChosen);
                ScheduleLogger.log("queue: " + queue);
                ScheduleLogger.log("choices: " + choices.eventChosen);
                ScheduleLogger.log("queue choices: " + choices.senderChoice);
                // need to make sure this happens-before event is schedulable at i instead
                cond = canSchedule(queue, i);
                if (!cond.isConstFalse()) // if it is enabled at i
                    return choices.senderChoice.guard(cond); // run the happens-before event
            }
        }
        ScheduleLogger.log("couldn't get backtrack for");
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
