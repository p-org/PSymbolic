package symbolicp.runtime;

import symbolicp.bdd.Bdd;

public abstract class EventHandler {
    public final EventTag eventTag;

    protected EventHandler(EventTag eventTag) {
        this.eventTag = eventTag;
    }

    public abstract void handleEvent(
        Bdd pc,
        Object payload,
        BaseMachine machine,
        GotoOutcome gotoOutcome,
        RaiseOutcome raiseOutcome
    );
}
