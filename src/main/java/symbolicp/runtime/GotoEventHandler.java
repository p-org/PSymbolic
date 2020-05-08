package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.ValueSummaryOps;

public class GotoEventHandler extends EventHandler {
    public final StateTag dest;
    public final ValueSummaryOps payloadOps;

    public GotoEventHandler(EventTag eventTag, StateTag dest, ValueSummaryOps payloadOps) {
        super(eventTag);
        this.dest = dest;
        this.payloadOps = payloadOps;
    }

    public GotoEventHandler(EventTag eventTag, StateTag dest) {
        this(eventTag, dest, null);
    }

    public void transitionAction(Bdd pc, BaseMachine machine, Object payload) {}

    @Override
    public void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome gotoOutcome,
                            RaiseOutcome raiseOutcome) {
        transitionAction(pc, machine, payload);
        // If (payload != null) then we should have (payloadOps != null)
        assert payload == null || payloadOps != null;
        gotoOutcome.addGuardedGoto(pc, dest, payloadOps, payload);
    }
}
