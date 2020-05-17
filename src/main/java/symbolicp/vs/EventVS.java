package symbolicp.vs;

import symbolicp.util.NotImplementedException;
import symbolicp.bdd.Bdd;
import symbolicp.runtime.EventTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventVS {
    private PrimVS<EventTag> tag;
    private Map<EventTag, Object> payloads;

    public EventVS(PrimVS<EventTag> tag, Map<EventTag, Object> payloads) {
        this.tag = tag;
        this.payloads = payloads;
    }

    public EventVS(Bdd pc, EventTag tag, Object payload) {
        this.tag = new PrimVS.Ops<EventTag>().guard(new PrimVS<>(tag), pc);
        this.payloads = new HashMap<>();
        this.payloads.put(tag, payload);
    }

    public PrimVS<EventTag> getTag() {
        return tag;
    }

    public Object getPayload(EventTag eventTag) {
        return payloads.get(eventTag);
    }

    public static class Ops implements ValueSummaryOps<EventVS> {
        private final PrimVS.Ops<EventTag> tagOps;
        private final Map<EventTag, ValueSummaryOps> payloadOps;

        public Ops(Object... tagsAndOps) {
            tagOps = new PrimVS.Ops<>();
            payloadOps = new HashMap<>();
            for (int i = 0; i < tagsAndOps.length; i += 2) {
                EventTag tag = (EventTag) tagsAndOps[i];
                ValueSummaryOps opsForTag = (ValueSummaryOps) tagsAndOps[i + 1];
                payloadOps.put(tag, opsForTag);
            }
        }

        @Override
        public boolean isEmpty(EventVS eventVS) {
            return tagOps.isEmpty(eventVS.tag);
        }

        @Override
        public EventVS empty() {
            return new EventVS(tagOps.empty(), new HashMap<>());
        }

        @Override
        public EventVS guard(EventVS eventVS, Bdd guard) {
            final PrimVS<EventTag> newTag = tagOps.guard(eventVS.tag, guard);
            final Map<EventTag, Object> newPayloads = new HashMap<>();
            for (Map.Entry<EventTag, Object> entry : eventVS.payloads.entrySet()) {
                final EventTag tag = entry.getKey();
                final Object payload = entry.getValue();
                if (newTag.guardedValues.containsKey(tag)) {
                    if (payload == null) {
                        assert payloadOps.get(tag) == null;
                        newPayloads.put(tag, null);
                    } else {
                        newPayloads.put(tag, payloadOps.get(tag).guard(payload, guard));
                    }
                }
            }
            return new EventVS(newTag, newPayloads);
        }

        @Override
        public EventVS merge(Iterable<EventVS> events) {
            final List<PrimVS<EventTag>> tagsToMerge = new ArrayList<>();
            final Map<EventTag, List<Object>> payloadsToMerge = new HashMap<>();
            for (EventVS event : events) {
                tagsToMerge.add(event.tag);
                for (Map.Entry<EventTag, Object> entry : event.payloads.entrySet()) {
                    payloadsToMerge
                        .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                        .add(entry.getValue());
                }
            }

            final PrimVS<EventTag> newTag = tagOps.merge(tagsToMerge);
            final Map<EventTag, Object> newPayloads = new HashMap<>();
            for (Map.Entry<EventTag, List<Object>> entry : payloadsToMerge.entrySet()) {
                EventTag tag = entry.getKey();
                ValueSummaryOps ops = payloadOps.get(tag);
                if (ops == null) {
                    newPayloads.put(tag, null);
                } else {
                    Object newPayload = payloadOps.get(tag).merge(entry.getValue());
                    newPayloads.put(tag, newPayload);
                }
            }

            return new EventVS(newTag, newPayloads);
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(EventVS left, EventVS right, Bdd pc) {
            throw new NotImplementedException();
        }
    }

    @Override
    public String toString() {
        return tag.guardedValues.toString();
    }
}
