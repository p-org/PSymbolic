package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.ValueSummary;

public class GotoEventHandler extends EventHandler {
    public final State dest;

    public GotoEventHandler(EventName eventName, State dest) {
        super(eventName);
        this.dest = dest;
    }

    public void transitionAction(Bdd pc, Machine machine, Object payload) {}

    @Override
    public void handleEvent(Bdd pc, ValueSummary payload, Machine machine, GotoOutcome gotoOutcome,
                            RaiseOutcome raiseOutcome) {
        transitionAction(pc, machine, payload);
        assert payload == null;
        gotoOutcome.addGuardedGoto(pc, dest, payload);
    }
}
