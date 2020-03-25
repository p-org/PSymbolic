package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;

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
}
