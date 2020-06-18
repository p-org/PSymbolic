package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.util.Checks;
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
    }

    public void addHandlers(EventHandler... eventHandlers) {
        for (EventHandler handler : eventHandlers) {
            this.eventHandlers.put(handler.eventName, handler);
        }
    }

    public void handleEvent(Event event, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
        for (GuardedValue<EventName> entry : event.getName().getGuardedValues()) {
            EventName name = entry.value;
            Bdd eventPc = entry.guard;
            ScheduleLogger.handle(this, event);
            if (eventHandlers.containsKey(name)) {
                eventHandlers.get(name).handleEvent(
                        eventPc,
                        event.guard(eventPc).getPayload(),
                        machine,
                        gotoOutcome,
                        raiseOutcome
                        );
            }
            else {
                throw new BugFoundException("State " + this.name + " missing handler for event: " + name, eventPc);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("State %s#%d", name, id);
    }
}
