package symbolicp.runtime;

import symbolicp.bdd.Bdd;

public class GotoEventHandler<StateTag, EventTag> extends EventHandler<StateTag, EventTag> {
    public final StateTag dest;

    public GotoEventHandler(EventTag eventTag, StateTag dest) {
        super(eventTag);
        this.dest = dest;
    }

    public void transitionAction(Bdd pc, BaseMachine machine, Object payload) {}

    @Override
    public void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome,
                            RaiseOutcome<EventTag> raiseOutcome) {
        transitionAction(pc, machine, payload);
        gotoOutcome.addGuardedGoto(pc, dest);
    }
}
