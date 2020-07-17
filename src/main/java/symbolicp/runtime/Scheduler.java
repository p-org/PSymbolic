package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.run.Assert;
import symbolicp.vs.GuardedValue;
import symbolicp.vs.IntUtils;
import symbolicp.vs.PrimVS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Scheduler implements SymbolicSearch {

    /** The scheduling choices made */
    public final Schedule schedule;

    /** List of all machines along any path constraints */
    final List<Machine> machines;

    /** How many instances of each Machine there are */
    final Map<Class<? extends Machine>, PrimVS<Integer>> machineCounters;

    /** The machine to start with */
    private Machine start;

    /** Current depth of exploration */
    private int depth = 0;
    /** Whether or not search is done */
    private boolean done = false;

    /** Maximum number of internal steps allowed */
    private int maxInternalSteps = -1;
    /** Maximum depth to explore */
    private int maxDepth = -1;
    /** Maximum depth to explore before considering it an error */
    private int errorDepth = -1;

    /** Find out whether symbolic execution is finished
     * @return Whether or not there are more steps to run
     */
    public boolean isDone() {
        return done || depth == maxDepth;
    }

    /** Get the machine that execution started with
     * @return The starting machine
     */
    public Machine getStartMachine() {
        return start;
    }

    /** Get current depth
     * @return current depth
     */
    public int getDepth() { return depth; }

    /** Get the schedule
     * @return The schedule
     */
    public Schedule getSchedule() { return schedule; }

    /** Make a new Scheduler
     * @param machines The machines initially in the Scheduler
     */
    public Scheduler(Machine... machines) {
        //ScheduleLogger.disable();
        schedule = new Schedule();
        this.machines = new ArrayList<>();
        this.machineCounters = new HashMap<>();

        for (Machine machine : machines) {
            this.machines.add(machine);
            if (this.machineCounters.containsKey(machine.getClass())) {
                this.machineCounters.put(machine.getClass(),
                        IntUtils.add(this.machineCounters.get(machine.getClass()), 1));
            } else {
                this.machineCounters.put(machine.getClass(), new PrimVS<>(1));
            }
            ScheduleLogger.onCreateMachine(Bdd.constTrue(), machine);
            machine.setScheduler(this);
            schedule.makeMachine(machine, Bdd.constTrue());
        }
    }

    public void setMaxInternalSteps(int maxSteps) { this.maxInternalSteps = maxSteps; }

    public int getMaxInternalSteps() { return maxInternalSteps; }

    @Override
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public PrimVS<Integer> getNextInteger(int bound, Bdd pc) {
        List<PrimVS> choices = new ArrayList<>();
        for (int i = 0; i < bound; i++) {
            choices.add(new PrimVS<>(i));
        }
        PrimVS<Integer> res = (PrimVS<Integer>) NondetUtil.getNondetChoice(choices);
        schedule.scheduleIntChoice(res.guard(pc));
        return res;
    }

    @Override
    public PrimVS<Boolean> getNextBoolean(Bdd pc) {
        List<PrimVS> choices = new ArrayList<>();
        choices.add(new PrimVS<>(true));
        choices.add(new PrimVS<>(false));
        PrimVS<Boolean> res = (PrimVS<Boolean>) NondetUtil.getNondetChoice(choices);
        schedule.scheduleBoolChoice(res.guard(pc));
        return res;
    }

    @Override
    public void setErrorDepth(int errorDepth) {
        this.errorDepth = errorDepth;
    }

    /** Start execution with the specified machine
     * @param machine Machine to start execution with */
    public void startWith(Machine machine) {
        if (this.machineCounters.containsKey(machine.getClass())) {
            this.machineCounters.put(machine.getClass(),
                    IntUtils.add(this.machineCounters.get(machine.getClass()), 1));
        } else {
            this.machineCounters.put(machine.getClass(), new PrimVS<>(1));
        }

        machines.add(machine);
        start = machine;
        ScheduleLogger.onCreateMachine(Bdd.constTrue(), machine);
        machine.setScheduler(this);
        schedule.makeMachine(machine, Bdd.constTrue());

        performEffect(
                new Event(
                        EventName.Init.instance,
                        new PrimVS<>(machine),
                        null
                )
        );
    }

    @Override
    public void doSearch(Machine target) {
        startWith(target);
        while (!isDone()) {
            // ScheduleLogger.log("step " + depth + ", true queries " + Bdd.trueQueries + ", false queries " + Bdd.falseQueries);
            Assert.prop(errorDepth < 0 || depth < errorDepth, "Maximum allowed depth " + errorDepth + " exceeded", this, schedule.getLengthCond(schedule.size()));
            step();
        }
    }

    public PrimVS<Machine> getNextSender() {
        List<PrimVS> candidateSenders = new ArrayList<>();

        for (Machine machine : machines) {
            if (!machine.sendEffects.isEmpty()) {
                Bdd initCond = machine.sendEffects.enabledCondInit().getGuard(true);
                if (!initCond.isConstFalse()) {
                    return new PrimVS<>(machine).guard(initCond);
                }
                Bdd canRun = machine.sendEffects.enabledCond(Event::canRun).getGuard(true);
                if (!canRun.isConstFalse()) {
                    candidateSenders.add(new PrimVS<>(machine).guard(canRun));
                }
            }
        }

        PrimVS<Machine> choices = (PrimVS<Machine>) NondetUtil.getNondetChoice(candidateSenders);
        schedule.scheduleSender(choices);
        return choices;
    }

    public void step() {
        PrimVS<Machine> choices = getNextSender();

        if (choices.isEmptyVS()) {
            ScheduleLogger.finished(depth);
            done = true;
            return;
        }

        for (GuardedValue<Machine> sender : choices.getGuardedValues()) {
            Machine machine = sender.value;
            Bdd guard = sender.guard;
            Event effect = machine.sendEffects.remove(guard);
            ScheduleLogger.schedule(depth, effect);
            PrimVS<State> oldState = machine.getState();
            //assert(effect.getUniverse().implies(guard).isConstTrue());
            performEffect(effect);
            // After the effect is performed, the machine state should be the same under all other path conditions
            //assert(Checks.equalUnder(oldState, machine.getState(), machine.getState().guard(guard.not()).getUniverse()));
        }

        depth++;
    }

    public PrimVS<Machine> allocateMachine(Bdd pc, Class<? extends Machine> machineType,
                                           Function<Integer, ? extends Machine> constructor) {
        if (!machineCounters.containsKey(machineType)) {
            machineCounters.put(machineType, new PrimVS<>(0));
        }
        PrimVS<Integer> guardedCount = machineCounters.get(machineType).guard(pc);

        Machine newMachine;
        newMachine = constructor.apply(IntUtils.maxValue(guardedCount));

        if (!machines.contains(newMachine)) {
            machines.add(newMachine);
        }

        ScheduleLogger.onCreateMachine(pc, newMachine);
        newMachine.setScheduler(this);
        schedule.makeMachine(newMachine, pc);

        guardedCount = IntUtils.add(guardedCount, 1);
        PrimVS<Integer> mergedCount = machineCounters.get(machineType).update(pc, guardedCount);
        machineCounters.put(machineType, mergedCount);
        return new PrimVS<>(newMachine).guard(pc);
    }

    void performEffect(Event event) {
        for (GuardedValue<Machine> target : event.getMachine().getGuardedValues()) {
            Assert.prop(IntUtils.maxValue(target.value.getStack().size()) < 5, "Stack size exceeded 5", this, target.value.getStack().size().getGuard(5));
            target.value.processEventToCompletion(target.guard, event.guard(target.guard));
        }
    }

    public void disableLogging() {
        ScheduleLogger.disable();
    }

    public void enableLogging() {
        ScheduleLogger.enable();
    }
}
