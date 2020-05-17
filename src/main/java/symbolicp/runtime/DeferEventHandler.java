package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;

public class DeferEventHandler extends EventHandler {

    public DeferEventHandler(EventTag eventTag) {
        super(eventTag);
    }

    @Override
    public void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome gotoOutcome,
                            RaiseOutcome raiseOutcome) {
        // Push event to defer queue
    }
}