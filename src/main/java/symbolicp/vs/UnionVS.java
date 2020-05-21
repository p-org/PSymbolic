package symbolicp.vs;

import symbolicp.bdd.Bdd;
import symbolicp.runtime.Tag;
import symbolicp.util.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnionVS <T extends Tag> {

    private PrimVS<T> tag;
    private Map<T, Object> payloads;

    public UnionVS(PrimVS<T> tag, Map<T, Object> payloads) {
        this.tag = tag;
        this.payloads = payloads;
    }

    public UnionVS(Bdd pc, T tag, Object payloads) {
        this.tag = new PrimVS.Ops<T>().guard(new PrimVS<>(tag), pc);
        this.payloads = new HashMap<>();
        this.payloads.put(tag, payloads);
    }

    public PrimVS<T> getTag() {
        return tag;
    }

    public Object getPayload(T tag) {
        return payloads.get(tag);
    }

    public static class Ops<T extends Tag> implements ValueSummaryOps<UnionVS<T>> {
        private final PrimVS.Ops<T> tagOps;
        private final Map<T, ValueSummaryOps> payloadOps;

        public Ops(Object... tagsAndOps) {
            tagOps = new PrimVS.Ops<>();
            payloadOps = new HashMap<>();
            for (int i = 0; i < tagsAndOps.length; i += 2) {
                T tag = (T) tagsAndOps[i];
                ValueSummaryOps opsForTag = (ValueSummaryOps) tagsAndOps[i + 1];
                payloadOps.put(tag, opsForTag);
            }
        }

        @Override
        public boolean isEmpty(UnionVS<T> unionVS) {
            return tagOps.isEmpty(unionVS.tag);
        }

        @Override
        public UnionVS<T> empty() {
            return new UnionVS<>(tagOps.empty(), new HashMap<>());
        }

        @Override
        public UnionVS<T> guard(UnionVS<T> unionVS, Bdd guard) {
            final PrimVS<T> newTag = tagOps.guard(unionVS.tag, guard);
            final Map<T, Object> newPayloads = new HashMap<>();
            for (Map.Entry<T, Object> entry : unionVS.payloads.entrySet()) {
                final T tag = entry.getKey();
                final Object value = entry.getValue();
                if (newTag.guardedValues.containsKey(tag)) {
                    if (value == null) {
                        assert payloadOps.get(tag) == null;
                        payloadOps.put(tag, null);
                    } else {
                        newPayloads.put(tag, payloadOps.get(tag).guard(value, guard));
                    }
                }
            }
            return new UnionVS<>(newTag, newPayloads);
        }

        @Override
        public UnionVS<T> merge(Iterable<UnionVS<T>> unionVS) {
            final List<PrimVS<T>> tagsToMerge = new ArrayList<>();
            final Map<T, List<Object>> valuesToMerge = new HashMap<>();
            for (UnionVS<T> union : unionVS) {
                tagsToMerge.add(union.tag);
                for (Map.Entry<T, Object> entry : union.payloads.entrySet()) {
                    valuesToMerge
                            .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                            .add(entry.getValue());
                }
            }

            final PrimVS<T> newTag = tagOps.merge(tagsToMerge);
            final Map<T, Object> newPayloads = new HashMap<>();
            for (Map.Entry<T, List<Object>> entry : valuesToMerge.entrySet()) {
                T tag = entry.getKey();
                ValueSummaryOps ops = payloadOps.get(tag);
                if (ops == null) {
                    newPayloads.put(tag, null);
                } else {
                    Object newPayload = payloadOps.get(tag).merge(entry.getValue());
                    newPayloads.put(tag, newPayload);
                }
            }

            return new UnionVS<>(newTag, newPayloads);
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(UnionVS<T> left, UnionVS<T> right, Bdd pc) {
            throw new NotImplementedException();
        }
    }

}
