package symbolicp.runtime;

import symbolicp.bdd.Bdd;

public class GotoEventHandler extends EventHandler {
    public final StateTag dest;

    public GotoEventHandler(EventTag eventTag, StateTag dest) {
        super(eventTag);
        this.dest = dest;
    }

    public void transitionAction(Bdd pc, BaseMachine machine, Object payload) {}

    @Override
    public void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome gotoOutcome,
                            RaiseOutcome raiseOutcome) {
        transitionAction(pc, machine, payload);
        gotoOutcome.addGuardedGoto(pc, dest);
    }
}
