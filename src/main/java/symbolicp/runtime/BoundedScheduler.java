package symbolicp.runtime;

import org.checkerframework.checker.units.qual.A;
import symbolicp.bdd.Bdd;
import symbolicp.vs.GuardedValue;
import symbolicp.vs.IntUtils;
import symbolicp.vs.PrimVS;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

public class BoundedScheduler extends Scheduler {
    int iter = 0;
    int senderBound;
    private boolean isDoneIterating = false;

    public BoundedScheduler(int senderBound) {
        super();
        this.senderBound = senderBound;
    }

    @Override
    public void doSearch(Machine target) {
        while (!isDoneIterating) {
            ScheduleLogger.log("Iteration " + iter);
            super.doSearch(target);
            postIterationCleanup();
            iter++;
            if (iter > 10) return;
        }
    }

    public void postIterationCleanup() {
        ScheduleLogger.log("Cleanup!");
        for (int d = schedule.size() - 1; d >= 0; d--) {
            Schedule.Choices backtrack = schedule.backtrackChoice.get(d);
            schedule.clearRepeat(d);
            if (!backtrack.isEmpty()) {;
                for (Machine machine : schedule.getMachines()) {
                    machine.reset();
                }
                ScheduleLogger.log("backtrack to " + d);
                reset();
                return;
            } else {
                schedule.clearChoice(d);
            }
        }
        isDoneIterating = true;
    }

    @Override
    public void startWith(Machine machine) {
        super.startWith(machine);
/*
        if (iter == 0) {
            super.startWith(machine);
        } else {
            super.replayStartWith(machine);
        }
 */
    }

    private PrimVS getNext(int depth, int bound, Function<Integer, PrimVS> getRepeat, Function<Integer, List> getBacktrack,
                           Consumer<Integer> clearBacktrack, BiConsumer<PrimVS, Integer> addRepeat,
                           BiConsumer<List, Integer> addBacktrack, Supplier<List> getChoices,
                           Function<List, PrimVS> generateNext) {
        List choices = new ArrayList<>();
        if (depth < schedule.size()) {
            ScheduleLogger.log("repeat or backtrack");
            PrimVS repeat = getRepeat.apply(depth);
            if (!repeat.getUniverse().isConstFalse()) {
                return repeat;
            }
            ScheduleLogger.log("CHOSE FROM backtrack: " + getBacktrack.apply(depth));
            // nothing to repeat, so look at backtrack set
            choices = getBacktrack.apply(depth);
            clearBacktrack.accept(depth);
        }

        if (choices.isEmpty()) {
            // no choice to backtrack to, so generate new choices
            if (iter > 0)
                ScheduleLogger.log("new choice at depth " + depth);
            choices = getChoices.get();
        }

        ScheduleLogger.log("choose from " + choices);
        int limit = Math.min(choices.size(), bound);
        List chosen = choices.subList(0, limit);
        List backtrack = new ArrayList();
        if (limit < choices.size())
            backtrack = choices.subList(limit, choices.size());

        PrimVS chosenVS = generateNext.apply(chosen);
        ScheduleLogger.log("add repeat " + chosenVS);
        addRepeat.accept(chosenVS, depth);
        ScheduleLogger.log("add backtrack " + backtrack);
        if (backtrack.size() != 0) {
            ScheduleLogger.log("NEED TO BACKTRACK TO " + depth + ", remaining: " + backtrack);
        }
        addBacktrack.accept(backtrack, depth);
        return chosenVS;
    }

    @Override
    public PrimVS<Machine> getNextSender() {
        int depth = choiceDepth;
        int startSize = schedule.size();
        PrimVS<Machine> res = getNext(depth, senderBound, schedule::getRepeatSender, schedule::getBacktrackSender,
                schedule::clearBacktrack, schedule::addRepeatSender, schedule::addBacktrackSender, super::getNextSenderChoices,
                super::getNextSender);
        ScheduleLogger.log("choice: " + schedule.getRepeatSender(depth));
        ScheduleLogger.log("backtrack: " + schedule.getBacktrackSender(depth));
        ScheduleLogger.log("full choice: " + schedule.getSenderChoice(depth));
        /*
        for (GuardedValue<Machine> sender : schedule.getSenderBacktrack(depth).getGuardedValues()) {
            Machine machine = sender.value;
            Bdd guard = sender.guard;
            assert(guard.and(res.getUniverse()).isConstFalse());
            machine.sendEffects.remove(guard);
        }
         */
/*
        for (Machine machine : machines) {
            if (!machine.sendEffects.isEmpty()) {
                Bdd initCond = machine.sendEffects.enabledCondInit().getGuard(true);
                if (!initCond.isConstFalse()) {
                    return new PrimVS<>(machine).guard(initCond);
                }
                Bdd canRun = machine.sendEffects.enabledCond(Event::canRun).getGuard(true);
                if (!canRun.isConstFalse()) {
                    if (canRun.and(res.getUniverse()).isConstFalse()) {
                        machine.sendEffects.remove(canRun);
                    }
                }
            }
        }

 */
/*
        for (Machine machine : machines) {
            if (!machine.sendEffects.isEmpty()) {
                ScheduleLogger.log("nonempty send effect at machine: " + machine);
                Bdd initCond = machine.sendEffects.enabledCondInit().getGuard(true);
                if (!initCond.isConstFalse()) {
                    ScheduleLogger.log("peek init: " + machine.sendEffects.peek(initCond));
                }
                Bdd canRun = machine.sendEffects.enabledCond(Event::canRun).getGuard(true);
                if (!canRun.isConstFalse()) {
                    ScheduleLogger.log("peek canRun: " + machine.sendEffects.peek(canRun));
                    // ScheduleLogger.log("canRun: " + canRun);
                }
            } else {
                ScheduleLogger.log("empty send effect at machine: " + machine);
            }
        }

 */
        /*
        ScheduleLogger.log("depth: " + depth);
        ScheduleLogger.log("schedule size: " + schedule.size());
        ScheduleLogger.log("Backtrack: " + schedule.getSenderBacktrack(depth));
        ScheduleLogger.log("Leftover: " + schedule.getSender(depth).guard(res.getUniverse().not()));
        */
        choiceDepth = depth + 1;
        return res;
    }

    @Override
    public PrimVS<Boolean> getNextBoolean(Bdd pc) {
        int depth = choiceDepth;
        PrimVS<Boolean> res = getNext(depth, senderBound, schedule::getRepeatBool, schedule::getBacktrackBool,
                schedule::clearBacktrack, schedule::addRepeatBool, schedule::addBacktrackBool,
                () -> super.getNextBooleanChoices(pc), super::getNextBoolean);
        ScheduleLogger.log("choice: " + schedule.getBoolChoice(depth));
        choiceDepth = depth + 1;
        return res;
    }

    @Override
    public PrimVS<Integer> getNextInteger(int bound, Bdd pc) {
        int depth = choiceDepth;
        PrimVS<Integer> res = getNext(depth, senderBound, schedule::getRepeatInt, schedule::getBacktrackInt,
                schedule::clearBacktrack, schedule::addRepeatInt, schedule::addBacktrackInt,
                () -> super.getNextIntegerChoices(bound, pc), super::getNextInteger);
        choiceDepth = depth + 1;
        return res;
    }

    @Override
    public PrimVS<Machine> allocateMachine(Bdd pc, Class<? extends Machine> machineType,
                                           Function<Integer, ? extends Machine> constructor) {
        if (!machineCounters.containsKey(machineType)) {
            machineCounters.put(machineType, new PrimVS<>(0));
        }
        PrimVS<Integer> guardedCount = machineCounters.get(machineType).guard(pc);

        PrimVS<Machine> allocated;
        if (schedule.hasMachine(machineType, guardedCount, pc)) {
            assert (iter != 0);
            allocated = schedule.getMachine(machineType, guardedCount).guard(pc);
            assert(allocated.getValues().size() == 1);
            ScheduleLogger.onCreateMachine(pc, allocated.getValues().iterator().next());
            allocated.getValues().iterator().next().setScheduler(this);
            machines.add(allocated.getValues().iterator().next());
        }
        else {
            ScheduleLogger.log("NEW MACHINE");
            Machine newMachine;
            newMachine = constructor.apply(IntUtils.maxValue(guardedCount));

            if (!machines.contains(newMachine)) {
                machines.add(newMachine);
            }

            ScheduleLogger.onCreateMachine(pc, newMachine);
            newMachine.setScheduler(this);
            schedule.makeMachine(newMachine, pc);
            allocated = new PrimVS<>(newMachine).guard(pc);
        }

        guardedCount = IntUtils.add(guardedCount, 1);

        PrimVS<Integer> mergedCount = machineCounters.get(machineType).update(pc, guardedCount);
        machineCounters.put(machineType, mergedCount);
        return allocated;
    }

}
