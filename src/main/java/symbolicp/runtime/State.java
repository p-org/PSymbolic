package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.vs.EventVS;

import java.util.Map;

public abstract class State <StateTag, EventTag> {

    abstract void entry(Bdd pc, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome, RaiseOutcome<EventTag> raiseOutcome);
    abstract void exit(Bdd pc, BaseMachine machine);

    private Map <EventTag, EventHandler<StateTag, EventTag>> eventHandlers;

    public void handleEvent(EventVS<EventTag> eventVS, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome, RaiseOutcome<EventTag> raiseOutcome) {
        for (Map.Entry<EventTag, Bdd> entry : eventVS.getTag().guardedValues.entrySet()) {
            EventTag tag = entry.getKey();
            Bdd eventPc = entry.getValue();
            if (eventHandlers.containsKey(tag)) {
                eventHandlers.get(tag).handleEvent(
                        eventPc,
                        eventVS.getPayload(tag),
                        machine,
                        gotoOutcome,
                        raiseOutcome
                        );
            }
            else {
                throw new BugFoundException("Missing handler for event: " + tag, eventPc);
            }
        }
    }
}
