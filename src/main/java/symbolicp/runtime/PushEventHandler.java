package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.ValueSummary;

public class PushEventHandler extends EventHandler {
    public final State dest;

    public PushEventHandler(EventName eventName, State dest) {
        super(eventName);
        this.dest = dest;
    }

    @Override
    public void handleEvent(Bdd pc, ValueSummary payload, Machine machine, Outcome outcome) {
        outcome.addGuardedPush(pc, dest, payload);
    }
}
