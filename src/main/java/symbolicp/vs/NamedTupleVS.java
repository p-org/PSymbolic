package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.*;

public class NamedTupleVS implements ValueSummary<NamedTupleVS> {
    private final Map<String, ValueSummary> fields;

    private NamedTupleVS(Map<String, ValueSummary> fields) {
        this.fields = fields;
    }

    public NamedTupleVS(Object... namesAndFields) {
        fields = new HashMap<>();
        for (int i = 0; i < namesAndFields.length; i += 2) {
            String name = (String)namesAndFields[i];
            ValueSummary val = (ValueSummary)namesAndFields[i + 1];
            fields.put(name, val);
        }
    }

    public ValueSummary getField(String name) {
        return fields.get(name);
    }

    public NamedTupleVS setField(String name, ValueSummary val) {
        final HashMap<String, ValueSummary> resultFields = new HashMap<>(fields);
        resultFields.put(name, val);
        return new NamedTupleVS(resultFields);
    }

    @Override
    public boolean isEmpty() {
        // Optimization: named tuples should always be nonempty,
        // and all fields should exist under the same conditions.
        Map.Entry<String, ValueSummary> firstEntry = fields.entrySet().iterator().next();
        return (firstEntry.getValue().isEmpty());
    }

    @Override
    public NamedTupleVS guard(Bdd guard) {
        final Map<String, Object> resultFields = new HashMap<>();
        for (Map.Entry<String, ValueSummary> entry : fields.entrySet()) {
            resultFields.put(entry.getKey(), entry.getValue().guard(guard));
        }
        return new NamedTupleVS(resultFields);
    }

    @Override
    public NamedTupleVS merge(Iterable<NamedTupleVS> summaries) {
        final Map<String, ValueSummary> resultMap = new HashMap<>();
        final Set<String> fieldNames = fields.keySet();

        for (NamedTupleVS summary : summaries) {
            for (Map.Entry<String, ValueSummary> entry : summary.fields.entrySet()) {
                resultMap.computeIfPresent(entry.getKey(), (k, v) -> (summary.fields.get(entry.getValue())));
                resultMap.putIfAbsent(entry.getKey(), summary.fields.get(entry.getValue()));
            }
            fieldNames.addAll(summary.fields.keySet());
        }

        return new NamedTupleVS(resultMap);
    }

    @Override
    public NamedTupleVS merge(NamedTupleVS summaries) {
        return merge(Collections.singletonList(summaries));
    }

    @Override
    public NamedTupleVS update(Bdd guard, NamedTupleVS update) {
        return this.guard(guard.not()).merge(Collections.singletonList(update.guard(guard)));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(NamedTupleVS cmp, Bdd pc) {

        if (fields.keySet().equals(cmp.fields.keySet())) {
            return new PrimVS<>(false);
        }

        Bdd tupleEqual = fields.keySet().parallelStream()
                .map((s) -> fields.get(s).symbolicEquals(cmp.fields.get(s), pc).getGuard(Boolean.TRUE))
                .reduce(Bdd::and)
                .orElse(Bdd.constTrue());

        return BoolUtils.fromTrueGuard(pc.and(tupleEqual));
    }

    @Override
    public Bdd getUniverse() {
        // Optimization: named tuples should always be nonempty,
        // and all fields should exist under the same conditions.
        Map.Entry<String, ValueSummary> firstEntry = fields.entrySet().iterator().next();
        return (firstEntry.getValue().getUniverse());
    }
}
