package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.PrimVS;

public class DeferQueue extends SymbolicQueue<Event> {

    public DeferQueue() {
        super();
    }

    public void defer(Bdd pc, PrimVS<Event> event) {
        enqueueEntry(event.guard(pc));
    }
}
