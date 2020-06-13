package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.UnionVS;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummary;

import java.util.HashMap;
import java.util.Map;

public class RaiseOutcome {

    private Bdd cond;
    private Event event;

    public RaiseOutcome() {
        cond = Bdd.constFalse();
        event = new Event(new PrimVS<>(), new PrimVS<>());
        event.getPayload();
    }

    public boolean isEmpty() {
        return cond.isConstFalse();
    }

    public Bdd getRaiseCond() {
        return cond;
    }

    public Event getEventSummary() {
        return event;
    }

    public void addGuardedRaiseEvent(Event newEvent) {
        cond = cond.or(newEvent.getUniverse());
        event = event.merge(newEvent);
    }

    public void addGuardedRaise(Bdd pc, PrimVS<EventName> eventName, ValueSummary payload) {
        // TODO: Handle this in a more principled way
        if (eventName.getGuardedValues().size() != 1) {
            throw new RuntimeException("Raise statements with symbolically-determined event tags are not yet supported");
        }

        EventName nextEventName = eventName.getValues().iterator().next();

        if (payload != null) payload = payload.guard(pc);
        addGuardedRaiseEvent(new Event(nextEventName, new PrimVS<>(), payload).guard(pc));
    }

    public void addGuardedRaise(Bdd pc, PrimVS<EventName> eventName) {
        addGuardedRaise(pc, eventName, null);
    }
}
