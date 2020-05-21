package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.UnionVS;

public class DeferEventHandler extends EventHandler {

    public DeferEventHandler(EventTag eventTag) {
        super(eventTag);
    }

    @Override
    public void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome gotoOutcome,
                            RaiseOutcome raiseOutcome) {
        machine.deferredQueue.defer(pc, new UnionVS<>(pc, eventTag, payload));
    }
}