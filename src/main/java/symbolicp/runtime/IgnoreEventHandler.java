package symbolicp.runtime;

import symbolicp.bdd.Bdd;

public class IgnoreEventHandler<StateTag, EventTag> extends EventHandler<StateTag, EventTag> {

    public IgnoreEventHandler(EventTag eventTag) {
        super(eventTag);
    }

    @Override
    public void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome,
                            RaiseOutcome<EventTag> raiseOutcome) {
        // Ignore
    }
}
