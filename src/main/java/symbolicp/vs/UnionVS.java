package symbolicp.vs;

import symbolicp.bdd.Bdd;
import symbolicp.runtime.Tag;

import java.util.*;

/** Class for union value summaries */
public class UnionVS<T extends Tag> implements ValueSummary<UnionVS<T>> {

    private PrimVS<T> tag;
    private Map<T, ValueSummary> payloads;

    public UnionVS(PrimVS<T> tag, Map<T, ValueSummary> payloads) {
        this.tag = tag;
        this.payloads = payloads;
    }

    public UnionVS(Bdd pc, T tag, ValueSummary payloads) {
        this.tag = new PrimVS<>(tag).guard(pc);
        this.payloads = new HashMap<>();
        this.payloads.put(tag, payloads);
    }

    public UnionVS() {
        this.tag = new PrimVS<>();
        payloads = new HashMap<>();
    }

    public PrimVS<T> getTag() {
        return tag;
    }

    public ValueSummary getPayload(T tag) {
        return payloads.get(tag);
    }

    @Override
    public boolean isEmptyVS() {
        return tag.isEmptyVS();
    }

    @Override
    public UnionVS<T> guard(Bdd guard) {
        final PrimVS<T> newTag = tag.guard(guard);
        final Map<T, ValueSummary> newPayloads = new HashMap<>();
        for (Map.Entry<T, ValueSummary> entry : payloads.entrySet()) {
            final T tag = entry.getKey();
            final ValueSummary value = entry.getValue();
            if (!newTag.getGuard(tag).isConstFalse()) {
                newPayloads.put(tag, value.guard(guard));
            }
        }
        return new UnionVS<>(newTag, newPayloads);
    }

    @Override
    public UnionVS<T> merge(Iterable<UnionVS<T>> summaries) {
        final List<PrimVS<T>> tagsToMerge = new ArrayList<>();
        final Map<T, List<ValueSummary>> valuesToMerge = new HashMap<>();
        for (UnionVS<T> union : summaries) {
            tagsToMerge.add(union.tag);
            for (Map.Entry<T, ValueSummary> entry : union.payloads.entrySet()) {
                valuesToMerge
                        .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }

        final PrimVS<T> newTag = tag.merge(tagsToMerge);
        final Map<T, ValueSummary> newPayloads = new HashMap<>();
        for (Map.Entry<T, List<ValueSummary>> entry : valuesToMerge.entrySet()) {
            T tag = entry.getKey();
            List<ValueSummary> entryPayload = entry.getValue();
            if (entryPayload.size() > 0) {
                ValueSummary newPayload = entryPayload.get(0).merge(entryPayload.subList(1, entry.getValue().size()));
                newPayloads.put(tag, newPayload);
            }
        }

        return new UnionVS<>(newTag, newPayloads);
    }

    @Override
    public UnionVS<T> merge(UnionVS<T> summary) {
        return merge(Collections.singletonList(summary));
    }

    @Override
    public UnionVS<T> update(Bdd guard, UnionVS<T> update) {
        return null;
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(UnionVS<T> cmp, Bdd pc) {
        PrimVS<Boolean> res = tag.symbolicEquals(cmp.tag, pc);
        for (Map.Entry<T, ValueSummary> payload : cmp.payloads.entrySet()) {
            if (!payloads.containsKey(payload.getKey())) {
                PrimVS<Boolean> bothLackKey = BoolUtils.fromTrueGuard(pc.and(tag.getGuard(payload.getKey()).not()));
                res = BoolUtils.and(res, bothLackKey);
            } else {
                res = BoolUtils.and(res, payload.getValue().symbolicEquals(payloads.get(payload.getKey()), pc));
            }
        }
        return res;
    }

    @Override
    public Bdd getUniverse() {
        return tag.getUniverse();
    }

    @Override
    public String toString() {
        return tag.toString();
    }

}
