package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.Checks;
import symbolicp.vs.PrimVS;
import symbolicp.vs.UnionVS;
import symbolicp.vs.ValueSummary;

import java.util.HashMap;
import java.util.Map;

public class Outcome {

    private UnionVS outcome = new UnionVS();
    private Map<State, ValueSummary> payloads = new HashMap<>();
    private Bdd gotoCond = Bdd.constFalse();
    private Bdd raiseCond = Bdd.constFalse();
    private Bdd pushCond = Bdd.constFalse();
    private Bdd popCond = Bdd.constFalse();

    public boolean isEmpty() {
        return outcome.isEmptyVS();
    }

    public Bdd getRaiseCond() { return raiseCond; }

    public Event getEventSummary() { return (Event) outcome.getPayload(Event.class).guard(getRaiseCond()); }

    public void addGuardedRaiseEvent(Event newEvent) {
        outcome = outcome.merge(new UnionVS(newEvent));
        raiseCond = raiseCond.or(newEvent.getUniverse());
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

    public Bdd getGotoCond() { return gotoCond; }

    public PrimVS<State> getGotoStateSummary() { return (PrimVS<State>) outcome.getPayload(PrimVS.class).guard(getGotoCond()); }

    public Map<State, ValueSummary> getPayloads() {
        return payloads;
    }

    public void addGuardedGoto(Bdd pc, State newDest, ValueSummary newPayload) {
        outcome = outcome.merge(new UnionVS(new PrimVS<>(newDest).guard(pc)));
        if (newPayload != null) {
            payloads.merge(newDest, newPayload, (x, y) -> x.merge(y));
        }
        gotoCond = gotoCond.or(pc);
    }

    public void addGuardedGoto(Bdd pc, State newDest) {
        addGuardedGoto(pc, newDest, null);
    }

    public Bdd getPushCond() { return pushCond; }

    public PrimVS<State> getPushStateSummary() { return (PrimVS<State>) outcome.getPayload(PrimVS.class).guard(getPushCond()); }

    public void addGuardedPush(Bdd pc, State newDest, ValueSummary newPayload) {
        outcome = outcome.merge(new UnionVS(new PrimVS<>(newDest).guard(pc)));
        if (newPayload != null) {
            payloads.merge(newDest, newPayload, (x, y) -> x.merge(y));
        }
        pushCond = pushCond.or(pc);
    }

    public Bdd getPopCond() { return popCond; }

    public void addGuardedPop(Bdd pc) {
        popCond = popCond.or(pc);
    }

}
