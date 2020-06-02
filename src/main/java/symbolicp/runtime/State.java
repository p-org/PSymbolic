package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.vs.GuardedValue;
import symbolicp.vs.UnionVS;
import symbolicp.vs.ValueSummary;

import java.util.HashMap;
import java.util.Map;

public abstract class State {
    public final StateTag stateTag;
    private final Map<EventTag, EventHandler> eventHandlers;

    public void entry(Bdd pc, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {}
    public void exit(Bdd pc, BaseMachine machine) {}

    public State(StateTag stateTag, EventHandler... eventHandlers) {
        this.stateTag = stateTag;

        this.eventHandlers = new HashMap<>();
        for (EventHandler handler : eventHandlers) {
            this.eventHandlers.put(handler.eventTag, handler);
        }
    }

    public void handleEvent(UnionVS<EventTag> EventVS, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
        for (GuardedValue<EventTag> entry : EventVS.getTag().getGuardedValues()) {
            EventTag tag = entry.value;
            Bdd eventPc = entry.guard;
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
