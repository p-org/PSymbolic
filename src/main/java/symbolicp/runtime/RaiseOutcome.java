package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.UnionVS;
import symbolicp.vs.PrimVS;

import java.util.HashMap;
import java.util.Map;

public class RaiseOutcome {
    private final UnionVS.Ops<EventTag> eventOps;

    private Bdd cond;
    private UnionVS<EventTag> event;

    public RaiseOutcome(UnionVS.Ops<EventTag> eventOps) {
        this.eventOps = eventOps;
        cond = Bdd.constFalse();
        event = eventOps.empty();
    }

    public boolean isEmpty() {
        return cond.isConstFalse();
    }

    public Bdd getRaiseCond() {
        return cond;
    }

    public UnionVS<EventTag> getEventSummary() {
        return event;
    }

    public void addGuardedRaise(Bdd pc, UnionVS<EventTag> newEvent) {
        cond = cond.or(pc);
        event = eventOps.merge2(event, newEvent);
    }

    public void addGuardedRaise(Bdd pc, PrimVS<EventTag> eventTag, Object payload) {
        // TODO: Handle this in a more principled way
        if (eventTag.guardedValues.size() != 1) {
            throw new RuntimeException("Raise statements with symbolically-determined event tags are not yet supported");
        }

        EventTag tag = eventTag.guardedValues.keySet().iterator().next();

        Map<EventTag, Object> payloads = new HashMap<>();
        payloads.put(tag, payload);

        UnionVS<EventTag> EventVS = new UnionVS<>(eventTag, payloads);
        addGuardedRaise(pc, EventVS);
    }

    public void addGuardedRaise(Bdd pc, PrimVS<EventTag> eventTag) {
        addGuardedRaise(pc, eventTag, null);
    }
}
