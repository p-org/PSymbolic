package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;

public class RaiseOutcome<EventTag> {
    private final EventVS.Ops<EventTag> eventOps;

    private Bdd cond;
    private EventVS<EventTag> event;

    public RaiseOutcome(EventVS.Ops<EventTag> eventOps) {
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

    public EventVS<EventTag> getEventSummary() {
        return event;
    }

    public void addGuardedRaise(Bdd pc, EventVS<EventTag> newEvent) {
        cond = cond.or(pc);
        event = eventOps.merge2(event, newEvent);
    }
}
