package symbolicp.runtime;

import symbolicp.bdd.Bdd;

public abstract class EventHandler <StateTag, EventTag> {
    public final EventTag eventTag;

    protected EventHandler(EventTag eventTag) {
        this.eventTag = eventTag;
    }

    abstract void handleEvent(
        Bdd pc,
        Object payload,
        BaseMachine machine,
        GotoOutcome<StateTag> gotoOutcome,
        RaiseOutcome<EventTag> raiseOutcome
    );
}
