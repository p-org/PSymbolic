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

    @Override
    public PrimVS<Boolean> symbolicEquals(NamedTupleVS other, Bdd pc) {
        if (fields.keySet().equals(other.fields.keySet())) {
            return new PrimVS<>(false);
        }
        Bdd tupleEqual = fields.keySet().parallelStream()
                .map((s) -> (Bdd) VSOps.symbolicEquals(fields.get(s), other.fields.get(s), pc).guardedValues.get(Boolean.TRUE))
                .reduce(Bdd::and)
                .orElse(Bdd.constTrue());
        return BoolUtils.fromTrueGuard(pc.and(tupleEqual));
    }
}
