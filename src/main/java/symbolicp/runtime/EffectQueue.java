package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.Checks;
import symbolicp.util.NotImplementedException;
import symbolicp.vs.*;

import java.util.function.Function;

public class EffectQueue extends SymbolicQueue<Event> implements SenderCollection {

    public EffectQueue() {
        super();
    }

    public void send(Bdd pc, PrimVS<Machine> dest, PrimVS<EventName> eventName, ValueSummary payload) {
        assert(Checks.includedIn(pc));
        if (eventName.getGuardedValues().size() > 1) {
            throw new NotImplementedException();
        }
        enqueueEntry(new Event(eventName, dest, payload));
    }

    public PrimVS<Machine> create(
            Bdd pc,
            Scheduler scheduler,
            Class<? extends Machine> machineType,
            ValueSummary payload,
            Function<Integer, ? extends Machine> constructor
    ) {
        PrimVS<Machine> machine = scheduler.allocateMachine(pc, machineType, constructor);
        if (payload != null) payload = payload.guard(pc);
        enqueueEntry(new Event(EventName.Init.instance, machine, payload).guard(pc));
        return machine;
    }

}
