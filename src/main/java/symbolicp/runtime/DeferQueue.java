package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;

public class DeferQueue extends SymbolicQueue<DeferQueue.Event> {
    public static class Event implements SymbolicQueue.Entry<Event> {
        final EventVS.Ops eventOps;
        final Bdd cond;
        final EventVS event;

        public Event(EventVS.Ops eventOps, Bdd cond, EventVS event) {
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

    final EventVS.Ops eventOps;

    public DeferQueue(EventVS.Ops eventOps) {
        super();
        this.eventOps = eventOps;
    }

    public void defer(Bdd pc, EventVS event) {
        enqueueEntry(new Event(eventOps, pc, event));
    }
}
