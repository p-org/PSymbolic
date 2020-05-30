package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.UnionVS;
import symbolicp.vs.ValueSummary;

public class DeferEventHandler extends EventHandler {

    public DeferEventHandler(EventTag eventTag) {
        super(eventTag);
    }

    @Override
    public void handleEvent(Bdd pc, ValueSummary payload, BaseMachine machine, GotoOutcome gotoOutcome,
                            RaiseOutcome raiseOutcome) {
        machine.deferredQueue.defer(pc, new UnionVS<>(pc, eventTag, payload));
    }
}