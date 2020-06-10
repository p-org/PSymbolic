package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.NotImplementedException;
import symbolicp.vs.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EffectQueue extends SymbolicQueue<EffectQueue.Effect> {
    public abstract static class Effect implements SymbolicQueue.Entry {
        final PrimVS<Machine> target;

        public Effect(PrimVS<Machine> target) {
            this.target = target;
        }

        @Override
        public Bdd getCond() {
            return target.getUniverse();
        }
    }

    public static class SendEffect extends Effect {
        final PrimVS<Event> event;

        public SendEffect(Bdd cond, PrimVS<Machine> target, PrimVS<Event> event) {
            super(target);
            this.event = event;
        }

        @Override
        public Effect withCond(Bdd guard) {
            return new SendEffect(
                    guard,
                    target.guard(guard),
                    event.guard(guard)
            );
        }

        @Override
        public Bdd getCond() {
            Bdd startedCond = Bdd.constFalse();
            for (GuardedValue<Machine> m : target.getGuardedValues()) {
                startedCond.or(m.value.hasStarted().getGuard(true).and(m.guard));
            }
            return startedCond;
        }

        @Override
        public String toString() {
            return "SendEffect{" +
                    "target=" + target +
                    ", event=" + event +
                    '}';
        }
    }

    public static class InitEffect extends Effect {
        final ValueSummary payload;

        public InitEffect(Bdd cond, PrimVS<Machine> machine, ValueSummary payload) {
            super(machine.guard(cond));
            if (payload == null) this.payload = null;
            else this.payload = payload.guard(cond);
        }

        public InitEffect(Bdd cond, PrimVS<Machine> machine) {
            super(machine.guard(cond));
            payload = null;
        }

        @Override
        public Effect withCond(Bdd guard) {
                return new InitEffect(
                        guard,
                        target.guard(guard),
                        payload != null ? payload.guard(guard) : null
                );
        }

        @Override
        public String toString() {
            return "InitEffect{" +
                    "target=" + target +
                    '}';
        }
    }


    public EffectQueue() {
        super();
    }

    public void send(Bdd pc, PrimVS<Machine> dest, PrimVS<EventName> eventName, ValueSummary payload) {
        if (eventName.getGuardedValues().size() > 1) {
            throw new NotImplementedException();
        }
        PrimVS<Event> event = new PrimVS<Event>(new Event(eventName.getGuardedValues().get(0).value, payload));
        enqueueEntry(new SendEffect(pc, dest, event));
    }

    public PrimVS<Machine> create(
            Bdd pc,
            Scheduler scheduler,
            Class<? extends Machine> machineType,
            ValueSummary payload,
            Function<Integer, ? extends Machine> constructor
    ) {
        PrimVS<Machine> machine = scheduler.allocateMachine(pc, machineType, constructor);
        enqueueEntry(new InitEffect(pc, machine, payload));
        return machine;
    }

    public PrimVS<Machine> create(Bdd pc, Scheduler scheduler, Class<? extends Machine> machineType,
                                  Function<Integer, ? extends Machine> constructor) {
        return create(pc, scheduler, machineType, null, constructor);
    }
}
