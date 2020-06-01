package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.ValueSummary;

public class GotoEventHandler extends EventHandler {
    public final StateTag dest;

    public GotoEventHandler(EventTag eventTag, StateTag dest) {
        super(eventTag);
        this.dest = dest;
    }

    public void transitionAction(Bdd pc, BaseMachine machine, Object payload) {}

    @Override
    public void handleEvent(Bdd pc, ValueSummary payload, BaseMachine machine, GotoOutcome gotoOutcome,
                            RaiseOutcome raiseOutcome) {
        transitionAction(pc, machine, payload);
        assert payload == null;
        gotoOutcome.addGuardedGoto(pc, dest, payload);
    }
}
