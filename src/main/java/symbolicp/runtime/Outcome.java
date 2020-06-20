package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.PrimVS;
import symbolicp.vs.UnionVS;
import symbolicp.vs.ValueSummary;

import java.util.HashMap;
import java.util.Map;

public class Outcome {

    private UnionVS outcome;
    private Map<State, ValueSummary> payloads;

    public Outcome() {
        outcome = new UnionVS();
        payloads = new HashMap<>();
    }

    public boolean isEmpty() {
        return outcome.isEmptyVS();
    }

    public Bdd getRaiseCond() { return outcome.getUniverse(Event.class); }

    public Event getEventSummary() { return (Event) outcome.getPayload(Event.class); }

    public void addGuardedRaiseEvent(Event newEvent) {
        UnionVS makeNew = new UnionVS(newEvent);
        outcome = outcome.merge(new UnionVS(newEvent));
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

    public Bdd getGotoCond() { return outcome.getUniverse(PrimVS.class); }

    public PrimVS<State> getStateSummary() { return (PrimVS<State>) outcome.getPayload(PrimVS.class); }

    public Map<State, ValueSummary> getPayloads() {
        return payloads;
    }

    public void addGuardedGoto(Bdd pc, State newDest, ValueSummary newPayload) {
        outcome = outcome.merge(new UnionVS(new PrimVS<>(newDest).guard(pc)));

        if (newPayload != null) {
            payloads.merge(newDest, newPayload, (x, y) -> x.merge(y));
        }
    }

    public void addGuardedGoto(Bdd pc, State newDest) {
        addGuardedGoto(pc, newDest, null);
    }
}
