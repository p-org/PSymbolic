package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.PrimVS;
import symbolicp.vs.UnionVS;
import symbolicp.vs.ValueSummary;

public abstract class EventHandler {
    public final EventName eventName;

    protected EventHandler(EventName eventName) {
        this.eventName = eventName;
    }

    public Event makeEvent(ValueSummary payload) {
        return new Event(eventName, new PrimVS<>(), payload);
    }

    public abstract void handleEvent(
        Bdd pc,
        ValueSummary payload,
        Machine machine,
        GotoOutcome gotoOutcome,
        RaiseOutcome raiseOutcome
    );
}
