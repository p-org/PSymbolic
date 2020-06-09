package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.vs.GuardedValue;
import symbolicp.vs.PrimVS;
import symbolicp.vs.UnionVS;
import symbolicp.vs.ValueSummary;

import java.util.HashMap;
import java.util.Map;

public abstract class State extends HasId {
    private final Map<EventName, EventHandler> eventHandlers;

    public void entry(Bdd pc, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {}
    public void exit(Bdd pc, Machine machine) {}

    public State(String name, int id, EventHandler... eventHandlers) {
        super(name, id);

        this.eventHandlers = new HashMap<>();
        for (EventHandler handler : eventHandlers) {
            this.eventHandlers.put(handler.eventName, handler);
        }
    }

    public void handleEvent(PrimVS<Event> EventVS, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
        for (GuardedValue<Event> entry : EventVS.getGuardedValues()) {
            Event event = entry.value;
            Bdd eventPc = entry.guard;
            if (eventHandlers.containsKey(event.name)) {
                eventHandlers.get(event.name).handleEvent(
                        eventPc,
                        event.payload,
                        machine,
                        gotoOutcome,
                        raiseOutcome
                        );
            }
            else {
                throw new BugFoundException("Missing handler for event: " + event, eventPc);
            }
        }
    }
}
