package symbolicp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamedTupleVS {
    private final Map<String, Object> fields;

    private NamedTupleVS(Map<String, Object> fields) {
        this.fields = fields;
    }

    public NamedTupleVS(Object... namesAndFields) {
        fields = new HashMap<>();
        for (int i = 0; i < namesAndFields.length; i += 2) {
            String name = (String)namesAndFields[i];
            Object val = namesAndFields[i + 1];
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
            Map.Entry<String, Object> firstEntry = namedTupleVS.fields.entrySet().iterator().next();
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
                for (Map.Entry<String, Object> field : namedTupleVS.fields.entrySet()) {
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
