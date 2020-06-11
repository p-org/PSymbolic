package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.PrimVS;
import symbolicp.vs.UnionVS;

public class DeferQueue extends SymbolicQueue<DeferQueue.Entry> {
    public static class Entry implements SymbolicQueue.Entry {
        PrimVS<Event> event;

        public Entry(Bdd pc, PrimVS<Event> event) { this.event = new PrimVS<Event>(event).guard(pc); }

        @Override
        public Bdd getCond() {
            return event.getUniverse();
        }

        @Override
        public Entry withCond(Bdd cond) {
            return new Entry(cond, event.guard(cond));
        }
    }

    public DeferQueue() {
        super();
    }

    public void defer(Bdd pc, PrimVS<Event> event) {
        enqueueEntry(new Entry(pc, event.guard(pc)));
    }
}
