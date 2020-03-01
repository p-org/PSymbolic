package symbolicp.runtime;

import symbolicp.bdd.Bdd;

public abstract class GotoEventHandler<StateTag, EventTag> extends EventHandler<StateTag, EventTag> {
    public final StateTag dest;

    protected GotoEventHandler(EventTag eventTag, StateTag dest) {
        super(eventTag);
        this.dest = dest;
    }

    public abstract void transitionAction(Bdd pc, Object payload, BaseMachine machine);

    @Override
    public void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome,
                            RaiseOutcome<EventTag> raiseOutcome) {
        transitionAction(pc, payload, machine);
        gotoOutcome.addGuardedGoto(pc, dest);
    }
}
