package symbolicp.runtime;

import symbolicp.bdd.Bdd;

public abstract class EventHandler <StateTag, EventTag> {

    abstract void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome, RaiseOutcome<EventTag> raiseOutcome);
}
