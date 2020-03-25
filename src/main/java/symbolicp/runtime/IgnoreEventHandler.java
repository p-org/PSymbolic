package symbolicp.runtime;

import symbolicp.bdd.Bdd;

public class IgnoreEventHandler extends EventHandler {

    public IgnoreEventHandler(EventTag eventTag) {
        super(eventTag);
    }

    @Override
    public void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome gotoOutcome,
                            RaiseOutcome raiseOutcome) {
        // Ignore
    }
}
