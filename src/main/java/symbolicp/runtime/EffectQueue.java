package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;
import symbolicp.vs.MachineRefVS;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class EffectQueue<MachineTag, EventTag> {
    private final EventVS.Ops<EventTag> eventOps;

    public abstract static class Effect<MachineTag, EventTag> {
        final Bdd cond;
        final MachineRefVS<MachineTag> target;

        public Effect(Bdd cond, MachineRefVS<MachineTag> target) {
            this.cond = cond;
            this.target = target;
        }

        public abstract Effect<MachineTag, EventTag> withCond(EventVS.Ops<EventTag> eventOps, Bdd guard);
    }

    public static class SendEffect<MachineTag, EventTag> extends Effect<MachineTag, EventTag> {
        final EventVS<EventTag> event;

        public SendEffect(Bdd cond, MachineRefVS<MachineTag> target, EventVS<EventTag> event) {
            super(cond, target);
            this.event = event;
        }

        @Override
        public Effect<MachineTag, EventTag> withCond(EventVS.Ops<EventTag> eventOps, Bdd guard) {
            return new SendEffect<>(
                guard,
                new MachineRefVS.Ops<MachineTag>().guard(target, guard),
                eventOps.guard(event, guard));
        }
    }

    // TODO: Determine best architecture for creation effects

    private LinkedList<Effect<MachineTag, EventTag>> effects;

    public EffectQueue(EventVS.Ops<EventTag> eventOps) {
        this.eventOps = eventOps;
        this.effects = new LinkedList<>();
    }

    public void addEffect(Effect<MachineTag, EventTag> effect) {
        // TODO: We could do some merging here in the future
        effects.addLast(effect);
    }

    public boolean isEmpty() {
        return effects.isEmpty();
    }

    public List<Effect<MachineTag, EventTag>> dequeueEffect(Bdd pc) {
        List<Effect<MachineTag, EventTag>> result = new ArrayList<>();

        ListIterator<Effect<MachineTag, EventTag>> candidateIter = effects.listIterator();
        while (candidateIter.hasNext() && !pc.isConstFalse()) {
            Effect<MachineTag, EventTag> effect = candidateIter.next();
            Bdd dequeueCond = effect.cond.and(pc);
            if (!dequeueCond.isConstFalse()) {
                Bdd remainCond = effect.cond.and(pc.not());
                if (remainCond.isConstFalse()) {
                    candidateIter.remove();
                } else {
                    Effect<MachineTag, EventTag> remainingEffect = effect.withCond(eventOps, remainCond);
                    candidateIter.set(remainingEffect);
                }
                result.add(effect.withCond(eventOps, dequeueCond));

                // We only want to pop the first effect from the queue.  However, which event is "first" is
                // symbolically determined.  Even if the effect we just popped was first under the path constraint
                // 'dequeueCond', there may be a "residual" path constraint under which it does not exist, and
                // therefore a different effect is first in the queue.
                pc = pc.and(effect.cond.not());
            }
        }

        return result;
    }
}
