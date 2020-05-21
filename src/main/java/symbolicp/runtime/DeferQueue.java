package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.UnionVS;

public class DeferQueue extends SymbolicQueue<DeferQueue.Event> {
    public static class Event implements SymbolicQueue.Entry<Event> {
        final UnionVS.Ops<EventTag> eventOps;
        final Bdd cond;
        final UnionVS<EventTag> event;

        public Event(UnionVS.Ops<EventTag> eventOps, Bdd cond, UnionVS<EventTag> event) {
            this.eventOps = eventOps;
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
                    eventOps,
                    guard,
                    eventOps.guard(event, guard)
            );
        }
    }

    final UnionVS.Ops<EventTag> eventOps;

    public DeferQueue(UnionVS.Ops<EventTag> eventOps) {
        super();
        this.eventOps = eventOps;
    }

    public void defer(Bdd pc, UnionVS<EventTag> event) {
        enqueueEntry(new Event(eventOps, pc, event));
    }
}
