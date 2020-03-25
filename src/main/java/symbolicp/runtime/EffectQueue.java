package symbolicp.runtime;

import com.sun.xml.internal.rngom.parse.host.Base;
import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;
import symbolicp.vs.MachineRefVS;
import symbolicp.vs.PrimVS;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class EffectQueue {
    private final EventVS.Ops eventOps;

    public abstract static class Effect {
        final Bdd cond;
        final MachineRefVS target;

        public Effect(Bdd cond, MachineRefVS target) {
            this.cond = cond;
            this.target = target;
        }

        public abstract Effect withCond(EventVS.Ops eventOps, Bdd guard);
    }

    public static class SendEffect extends Effect {
        final EventVS event;

        public SendEffect(Bdd cond, MachineRefVS target, EventVS event) {
            super(cond, target);
            this.event = event;
        }

        @Override
        public Effect withCond(EventVS.Ops eventOps, Bdd guard) {
            return new SendEffect(
                guard,
                new MachineRefVS.Ops().guard(target, guard),
                eventOps.guard(event, guard));
        }
    }

    // TODO: Determine best architecture for creation effects
    public static class MachineCreationEffect extends Effect {
        final PrimVS<BaseMachine> machine;

        /** The compiler is responsible for pre-allocating machines and storing them in a machine VS, and assigning
         * them to any field variables (i.e. {Machine m = new Machine();})
         * This effect does not have a target
         * @param cond path constraint of this effect
         * @param machine PrimVS of machine to be created
         */
        public MachineCreationEffect(Bdd cond, PrimVS<BaseMachine> machine) {
            super(cond, null);
            this.machine = machine;
        }

        @Override
        public Effect withCond(EventVS.Ops eventOps, Bdd guard) {
            return new MachineCreationEffect(
                    guard,
                    new PrimVS.Ops<BaseMachine>().guard(machine, guard)
                    );
        }
    }

    private LinkedList<Effect> effects;

    public EffectQueue(EventVS.Ops eventOps) {
        this.eventOps = eventOps;
        this.effects = new LinkedList<>();
    }

    public void addEffect(Effect effect) {
        // TODO: We could do some merging here in the future
        effects.addLast(effect);
    }

    public boolean isEmpty() {
        return effects.isEmpty();
    }

    public List<Effect> dequeueEffect(Bdd pc) {
        List<Effect> result = new ArrayList<>();

        ListIterator<Effect> candidateIter = effects.listIterator();
        while (candidateIter.hasNext() && !pc.isConstFalse()) {
            Effect effect = candidateIter.next();
            Bdd dequeueCond = effect.cond.and(pc);
            if (!dequeueCond.isConstFalse()) {
                Bdd remainCond = effect.cond.and(pc.not());
                if (remainCond.isConstFalse()) {
                    candidateIter.remove();
                } else {
                    Effect remainingEffect = effect.withCond(eventOps, remainCond);
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
