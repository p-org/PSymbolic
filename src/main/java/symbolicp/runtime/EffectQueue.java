package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.NotImplementedException;
import symbolicp.vs.EventVS;
import symbolicp.vs.MachineRefVS;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummaryOps;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EffectQueue extends SymbolicQueue<EffectQueue.Effect> {
    public abstract static class Effect implements SymbolicQueue.Entry<Effect> {
        final EventVS.Ops eventOps;
        final Bdd cond;
        final MachineRefVS target;

        public Effect(EventVS.Ops eventOps, Bdd cond, MachineRefVS target) {
            this.eventOps = eventOps;
            this.cond = cond;
            this.target = target;
        }

        @Override
        public Bdd getCond() {
            return cond;
        }
    }

    public static class SendEffect extends Effect {
        final EventVS event;

        public SendEffect(EventVS.Ops eventOps, Bdd cond, MachineRefVS target, EventVS event) {
            super(eventOps, cond, target);
            this.event = event;
        }

        @Override
        public Effect withCond(Bdd guard) {
            return new SendEffect(
                    eventOps,
                    guard,
                    new MachineRefVS.Ops().guard(target, guard),
                    eventOps.guard(event, guard)
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

        public InitEffect(EventVS.Ops eventOps, ValueSummaryOps payloadOps, Bdd cond, MachineRefVS machine, Object payload) {
            super(eventOps, cond, machine);
            this.payloadOps = payloadOps;
            this.payload = payload;
        }

        public InitEffect(EventVS.Ops eventOps, Bdd cond, MachineRefVS machine) {
            this(eventOps, null, cond, machine, null);
        }

        @Override
        public Effect withCond(Bdd guard) {
                return new InitEffect(
                        eventOps,
                        payloadOps,
                        guard,
                        new MachineRefVS.Ops().guard(target, guard),
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

    private final EventVS.Ops eventOps;

    public EffectQueue(EventVS.Ops eventOps) {
        super();
        this.eventOps = eventOps;
    }

    public void send(Bdd pc, MachineRefVS dest, PrimVS<EventTag> eventTag, Object payload) {
        if (eventTag.guardedValues.size() > 1) {
            throw new NotImplementedException();
        }
        EventTag concreteTag = eventTag.guardedValues.keySet().iterator().next();
        Map<EventTag, Object> payloadMap = new HashMap<>();
        payloadMap.put(concreteTag, payload);
        enqueueEntry(new SendEffect(eventOps, pc, dest, new EventVS(eventTag, payloadMap)));
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
        enqueueEntry(new InitEffect(eventOps, payloadOps, pc, ref, payload));
        return ref;
    }

    public MachineRefVS create(Bdd pc, Scheduler scheduler, MachineTag tag, Function<Integer, BaseMachine> constructor) {
        return create( pc, scheduler, tag, null, null, constructor);
    }
}
