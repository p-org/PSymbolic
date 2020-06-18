package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.Checks;
import symbolicp.util.NotImplementedException;
import symbolicp.vs.PrimVS;
import symbolicp.vs.UnionVS;
import symbolicp.vs.ValueSummary;

import java.util.function.Function;

public interface EffectCollection {
    public void send(Bdd pc, PrimVS<Machine> dest, PrimVS<EventName> eventName, ValueSummary payload);

    public PrimVS<Machine> create(
            Bdd pc,
            Scheduler scheduler,
            Class<? extends Machine> machineType,
            ValueSummary payload,
            Function<Integer, ? extends Machine> constructor
    );

    public PrimVS<Integer> size();

    public boolean isEmpty();

    public void add(Event e);

    public Event remove(Bdd pc);

    public Event peek(Bdd pc);

    public PrimVS<Boolean> enabledCond(Function<Event, PrimVS<Boolean>> pred);

    default public PrimVS<Machine> create(Bdd pc, Scheduler scheduler, Class<? extends Machine> machineType,
                                  Function<Integer, ? extends Machine> constructor) {
        return create(pc, scheduler, machineType, null, constructor);
    }
}
