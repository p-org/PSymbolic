package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.UnionVS;

public class DeferQueue extends SymbolicQueue<DeferQueue.Event> {
    public static class Event implements SymbolicQueue.Entry<Event> {
        final Bdd cond;
        final UnionVS<EventTag> event;

        public Event(Bdd cond, UnionVS<EventTag> event) {
            this.cond = cond;
            this.event = event;
        }

        @Override
        public Bdd getCond() {
            return cond;
        }

        @Override
        public Event withCond(Bdd guard) {
            return new Event(
                    guard,
                    event.guard(guard)
            );
        }
    }

    public DeferQueue() {
        super();
    }

    public void defer(Bdd pc, UnionVS<EventTag> event) {
        enqueueEntry(new Event(pc, event));
    }
}
