package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.function.Function;

public class EffectBag extends SymbolicBag<Event> implements EffectCollection {

    public EffectBag() {
        super();
    }

    @Override
    public void send(Bdd pc, PrimVS<Machine> dest, PrimVS<EventName> eventName, ValueSummary payload) {
        ScheduleLogger.send(new Event(eventName, dest, payload).guard(pc));
        this.add(new Event(eventName, dest, payload).guard(pc));
    }

    @Override
    public PrimVS<Machine> create(Bdd pc, Scheduler scheduler, Class<? extends Machine> machineType, ValueSummary payload, Function<Integer, ? extends Machine> constructor) {
        PrimVS<Machine> machine = scheduler.allocateMachine(pc, machineType, constructor);
        if (payload != null) payload = payload.guard(pc);
        add(new Event(EventName.Init.instance, machine, payload).guard(pc));
        return machine;
    }

}
