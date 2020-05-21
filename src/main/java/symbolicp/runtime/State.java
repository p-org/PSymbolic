package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.vs.UnionVS;

import java.util.HashMap;
import java.util.Map;

public abstract class State {
    public final StateTag stateTag;
    private final Map<EventTag, EventHandler> eventHandlers;

    public void entry(Bdd pc, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, Object payload) {}
    public void exit(Bdd pc, BaseMachine machine) {}

    public State(StateTag stateTag, EventHandler... eventHandlers) {
        this.stateTag = stateTag;

        this.eventHandlers = new HashMap<>();
        for (EventHandler handler : eventHandlers) {
            this.eventHandlers.put(handler.eventTag, handler);
        }
    }

    public void handleEvent(UnionVS<EventTag> EventVS, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
        for (Map.Entry<EventTag, Bdd> entry : EventVS.getTag().guardedValues.entrySet()) {
            EventTag tag = entry.getKey();
            Bdd eventPc = entry.getValue();
            if (eventHandlers.containsKey(tag)) {
                eventHandlers.get(tag).handleEvent(
                        eventPc,
                        EventVS.getPayload(tag),
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
