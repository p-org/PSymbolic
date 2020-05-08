package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;
import symbolicp.vs.PrimVS;

import java.util.HashMap;
import java.util.Map;

public class RaiseOutcome {
    private final EventVS.Ops eventOps;

    private Bdd cond;
    private EventVS event;

    public RaiseOutcome(EventVS.Ops eventOps) {
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

    public EventVS getEventSummary() {
        return event;
    }

    public void addGuardedRaise(Bdd pc, EventVS newEvent) {
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

        EventVS eventVS = new EventVS(eventTag, payloads);
        addGuardedRaise(pc, eventVS);
    }

    public void addGuardedRaise(Bdd pc, PrimVS<EventTag> eventTag) {
        addGuardedRaise(pc, eventTag, null);
    }
}
