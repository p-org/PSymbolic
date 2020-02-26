package symbolicp.vs;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import symbolicp.bdd.Bdd;

import java.util.Map;

public class EventVS<EventTag> {
    private PrimVS<EventTag> tag;
    private Map<EventTag, Object> payloads;

    public PrimVS<EventTag> getTag() {
        return tag;
    }

    public Object getPayload(EventTag eventTag) {
        return payloads.get(eventTag);
    }

    public static class Ops<EventTag> implements ValueSummaryOps<EventVS<EventTag>> {

        @Override
        public boolean isEmpty(EventVS<EventTag> eventTagEventVS) {
            throw new NotImplementedException();
        }

        @Override
        public EventVS<EventTag> empty() {
            return null;
        }

        @Override
        public EventVS<EventTag> guard(EventVS<EventTag> eventTagEventVS, Bdd guard) {
            return null;
        }

        @Override
        public EventVS<EventTag> merge(Iterable<EventVS<EventTag>> eventVS) {
            return null;
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(EventVS<EventTag> left, EventVS<EventTag> right, Bdd pc) {
            return null;
        }
    }
}
