package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.ValueSummary;

public abstract class EventHandler {
    public final EventTag eventTag;

    protected EventHandler(EventTag eventTag) {
        this.eventTag = eventTag;
    }

    public abstract void handleEvent(
        Bdd pc,
        ValueSummary payload,
        BaseMachine machine,
        GotoOutcome gotoOutcome,
        RaiseOutcome raiseOutcome
    );
}
