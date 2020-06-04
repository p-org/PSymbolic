package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamedTupleVS implements ValueSummary<NamedTupleVS>{
    private final Map<String, ValueSummary> fields;

    private NamedTupleVS(Map<String, ValueSummary> fields) {
        this.fields = fields;
    }

    public NamedTupleVS(Object... namesAndFields) {
        fields = new HashMap<>();
        for (int i = 0; i < namesAndFields.length; i += 2) {
            String name = (String)namesAndFields[i];
            ValueSummary val = (ValueSummary) namesAndFields[i + 1];
            fields.put(name, val);
        }
    }

    public Object getField(String name) {
        return fields.get(name);
    }

    public NamedTupleVS setField(String name, Object val) {
        final HashMap<String, Object> resultFields = new HashMap<>(fields);
        resultFields.put(name, val);
        return new NamedTupleVS(resultFields);
    }

    @Override
    public NamedTupleVS guard(Bdd cond) {
        final Map<String, Object> resultFields = new HashMap<>();
        for (Map.Entry<String, ValueSummary> entry : fields.entrySet()) {
            final ValueSummary unguarded = entry.getValue();
            final Object guarded = VSOps.guard(unguarded, cond);
            resultFields.put(entry.getKey(), guarded);
        }
        return new NamedTupleVS(resultFields);
    }

    @Override
    public NamedTupleVS merge(NamedTupleVS other) {
        final Map<String, ValueSummary> resultFields = new HashMap<>();
        for (Map.Entry<String, ValueSummary> entry : fields.entrySet()) {
            ValueSummary merged = VSOps.merge2(entry.getValue(), other.fields.get(entry.getKey()));
            resultFields.put(entry.getKey(), merged);
        }
        return new NamedTupleVS(resultFields);
    }

    public static class Ops implements ValueSummaryOps<NamedTupleVS> {
        private final Map<String, ValueSummaryOps> fieldOps;

        public Ops(Object... namesAndFieldOps) {
            fieldOps = new HashMap<>();
            for (int i = 0; i < namesAndFieldOps.length; i += 2) {
                String name = (String)namesAndFieldOps[i];
                ValueSummaryOps ops = (ValueSummaryOps)namesAndFieldOps[i + 1];
                fieldOps.put(name, ops);
            }
        }

        @Override
        public boolean isEmpty(NamedTupleVS namedTupleVS) {
            // Optimization: named tuples should always be nonempty,
            // and all fields should exist under the same conditions.
            Map.Entry<String, ValueSummary> firstEntry = namedTupleVS.fields.entrySet().iterator().next();
            return (fieldOps.get(firstEntry.getKey()).isEmpty(firstEntry.getValue()));
        }

        @Override
        public NamedTupleVS empty() {
            final Map<String, Object> resultFields = new HashMap<>();
            for (Map.Entry<String, ValueSummaryOps> opsEntry : fieldOps.entrySet()) {
                resultFields.put(opsEntry.getKey(), opsEntry.getValue().empty());
            }
            return new NamedTupleVS(resultFields);
        }

        @Override
        public NamedTupleVS guard(NamedTupleVS namedTupleVS, Bdd guard) {
            final Map<String, Object> resultFields = new HashMap<>();
            for (Map.Entry<String, ValueSummaryOps> opsEntry : fieldOps.entrySet()) {
                final Object unguarded = namedTupleVS.fields.get(opsEntry.getKey());
                final Object guarded = opsEntry.getValue().guard(unguarded, guard);
                resultFields.put(opsEntry.getKey(), guarded);
            }
            return new NamedTupleVS(resultFields);
        }

        @Override
        public NamedTupleVS merge(Iterable<NamedTupleVS> namedTuplesToMerge) {
            final Map<String, List<Object>> resultFieldsToMerge = new HashMap<>();
            for (String fieldName : fieldOps.keySet()) {
                resultFieldsToMerge.put(fieldName, new ArrayList<>());
            }

            for (NamedTupleVS namedTupleVS : namedTuplesToMerge) {
                for (Map.Entry<String, ValueSummary> field : namedTupleVS.fields.entrySet()) {
                    resultFieldsToMerge.get(field.getKey()).add(field.getValue());
                }
            }

            final Map<String, Object> resultFields = new HashMap<>();
            for (Map.Entry<String, ValueSummaryOps> opsEntry : fieldOps.entrySet()) {
                Object merged = opsEntry.getValue().merge(resultFieldsToMerge.get(opsEntry.getKey()));
                resultFields.put(opsEntry.getKey(), merged);
            }

            return new NamedTupleVS(resultFields);
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(NamedTupleVS left, NamedTupleVS right, Bdd pc) {
            if (left.fields.keySet().equals(right.fields.keySet())) {
                return new PrimVS<>(false);
            }
            Bdd tupleEqual = left.fields.keySet().parallelStream()
                    .map((s) -> (Bdd) fieldOps.get(s).symbolicEquals(left.fields.get(s), right.fields.get(s), pc).guardedValues.get(Boolean.TRUE))
                    .reduce(Bdd::and)
                    .orElse(Bdd.constTrue());
            return BoolUtils.fromTrueGuard(pc.and(tupleEqual));
        }

        @Override
        public NamedTupleVS merge2(NamedTupleVS namedTupleVS1, NamedTupleVS namedTupleVS2) {
            final Map<String, Object> resultFields = new HashMap<>();
            for (Map.Entry<String, ValueSummaryOps> opsEntry : fieldOps.entrySet()) {
                Object merged =
                    opsEntry.getValue().merge2(
                        namedTupleVS1.fields.get(opsEntry.getKey()),
                        namedTupleVS2.fields.get(opsEntry.getKey())
                    );
                resultFields.put(opsEntry.getKey(), merged);
            }
            return new NamedTupleVS(resultFields);
        }
    }
}
