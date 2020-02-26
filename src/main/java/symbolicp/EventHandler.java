package symbolicp;

public abstract class EventHandler <StateTag, EventTag> {

    abstract void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome, RaiseOutcome<EventTag> raiseOutcome);
}
