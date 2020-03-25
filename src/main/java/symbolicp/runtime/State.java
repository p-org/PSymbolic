package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.vs.EventVS;

import java.util.HashMap;
import java.util.Map;

public abstract class State {
    public final StateTag stateTag;
    private final Map<EventTag, EventHandler> eventHandlers;

    public void entry(Bdd pc, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {}
    public void exit(Bdd pc, BaseMachine machine) {}

    public State(StateTag stateTag, EventHandler... eventHandlers) {
        this.stateTag = stateTag;

        this.eventHandlers = new HashMap<>();
        for (EventHandler handler : eventHandlers) {
            this.eventHandlers.put(handler.eventTag, handler);
        }
    }

    public void handleEvent(EventVS eventVS, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
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
