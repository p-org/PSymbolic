package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.NotImplementedException;
import symbolicp.vs.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EffectQueue extends SymbolicQueue<EffectQueue.Effect> {
    public abstract static class Effect implements SymbolicQueue.Entry<Effect> {
        final Bdd cond;
        final MachineRefVS target;

        public Effect(Bdd cond, MachineRefVS target) {
            this.cond = cond;
            this.target = target;
        }

        @Override
        public Bdd getCond() {
            return cond;
        }
    }

    public static class SendEffect extends Effect {
        final UnionVS<EventTag> event;

        public SendEffect(Bdd cond, MachineRefVS target, UnionVS<EventTag> event) {
            super(cond, target);
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
        public String toString() {
            return "SendEffect{" +
                    "target=" + target +
                    ", event=" + event +
                    '}';
        }
    }

    public static class InitEffect extends Effect {
        final ValueSummaryOps payloadOps;
        final Object payload;

        public InitEffect(ValueSummaryOps payloadOps, Bdd cond, MachineRefVS machine, Object payload) {
            super(cond, machine);
            this.payloadOps = payloadOps;
            this.payload = payload;
        }

        public InitEffect(Bdd cond, MachineRefVS machine) {
            this(null, cond, machine, null);
        }

        @Override
        public Effect withCond(Bdd guard) {
                return new InitEffect(
                        payloadOps,
                        guard,
                        target.guard(guard),
                        payload != null ? payloadOps.guard(payload, guard) : null
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

    public void send(Bdd pc, MachineRefVS dest, PrimVS<EventTag> eventTag, ValueSummary payload) {
        if (eventTag.guardedValues.size() > 1) {
            throw new NotImplementedException();
        }
        EventTag concreteTag = eventTag.guardedValues.keySet().iterator().next();
        Map<EventTag, ValueSummary> payloadMap = new HashMap<>();
        payloadMap.put(concreteTag, payload);
        enqueueEntry(new SendEffect(pc, dest, new UnionVS<EventTag>(eventTag, payloadMap)));
    }

    public MachineRefVS create(
            Bdd pc,
            Scheduler scheduler,
            MachineTag tag,
            ValueSummaryOps payloadOps,
            Object payload,
            Function<Integer, BaseMachine> constructor
    ) {
        MachineRefVS ref = scheduler.allocateMachineId(pc, tag, constructor);
        enqueueEntry(new InitEffect(payloadOps, pc, ref, payload));
        return ref;
    }

    public MachineRefVS create(Bdd pc, Scheduler scheduler, MachineTag tag, Function<Integer, BaseMachine> constructor) {
        return create( pc, scheduler, tag, null, null, constructor);
    }
}
