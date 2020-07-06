package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.IntUtils;
import symbolicp.vs.PrimVS;

import java.util.function.Function;

public class ReplayScheduler extends Scheduler {

    /** Schedule to replay */
    private final Schedule schedule;
    int boolChoiceDepth = 0;
    int intChoiceDepth = 0;

    public ReplayScheduler (Schedule schedule) {
        this(schedule, Bdd.constTrue());
    }

    public ReplayScheduler (Schedule schedule, Bdd pc) {
        ScheduleLogger.enable();
        this.schedule = schedule.guard(pc).getSingleSchedule();
        for (Machine machine : schedule.getMachines()) {
            machine.reset();
        }
    }

    @Override
    public boolean isDone() {
        return super.isDone() || this.getDepth() >= schedule.size();
    }

    @Override
    public void startWith(Machine machine) {
        PrimVS<Machine> machineVS;
        if (this.machineCounters.containsKey(machine.getClass())) {
            machineVS = schedule.getMachine(machine.getClass(), this.machineCounters.get(machine.getClass()));
            this.machineCounters.put(machine.getClass(),
                    IntUtils.add(this.machineCounters.get(machine.getClass()), 1));
        } else {
            machineVS = schedule.getMachine(machine.getClass(), new PrimVS<>(0));
            this.machineCounters.put(machine.getClass(), new PrimVS<>(1));
        }

        ScheduleLogger.onCreateMachine(machineVS.getUniverse(), machine);
        machine.setScheduler(this);

        performEffect(
                new Event(
                        EventName.Init.instance,
                        machineVS,
                        null
                )
        );
    }

    @Override
    public PrimVS<Machine> getNextSender() {
        return schedule.getSender(getDepth());
    }

    @Override
    public PrimVS<Boolean> getNextBoolean(Bdd pc) {
        PrimVS<Boolean> res = schedule.getBoolChoice(boolChoiceDepth);
        boolChoiceDepth++;
        return res;
    }

    @Override
    public PrimVS<Integer> getNextInteger(int bound, Bdd pc) {
        PrimVS<Integer> res = schedule.getIntChoice(intChoiceDepth);
        assert(IntUtils.maxValue(res) < bound);
        intChoiceDepth++;
        return res;
    }

    @Override
    public PrimVS<Machine> allocateMachine(Bdd pc, Class<? extends Machine> machineType,
                                           Function<Integer, ? extends Machine> constructor) {
        if (!machineCounters.containsKey(machineType)) {
            machineCounters.put(machineType, new PrimVS<>(0));
        }
        PrimVS<Integer> guardedCount = machineCounters.get(machineType).guard(pc);

        PrimVS<Machine> allocated = schedule.getMachine(machineType, guardedCount);
        ScheduleLogger.onCreateMachine(pc, allocated.getValues().iterator().next());
        allocated.getValues().iterator().next().setScheduler(this);

        guardedCount = IntUtils.add(guardedCount, 1);

        PrimVS<Integer> mergedCount = machineCounters.get(machineType).update(pc, guardedCount);
        machineCounters.put(machineType, mergedCount);
        return allocated;
    }
}
