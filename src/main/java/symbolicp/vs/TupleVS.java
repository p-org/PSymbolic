package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class TupleVS implements ValueSummary<TupleVS> {
    private final ValueSummary[] fields;

    public TupleVS(ValueSummary... items) {
        this.fields = items;
    }

    @Deprecated
    public TupleVS(Object... fields) {this.fields = (ValueSummary[]) fields;}

    public Object getField(int i) {
        return fields[i];
    }

    public TupleVS setField(int i, ValueSummary val) {
        final ValueSummary[] newItems = new ValueSummary[fields.length];
        System.arraycopy(fields, 0, newItems, 0, fields.length);
        newItems[i] = val;
        return new TupleVS(newItems);
    }

    @Override
    public TupleVS guard(Bdd cond) {
        final ValueSummary[] newItems = new ValueSummary[fields.length];
        for (int i = 0; i < fields.length; i++) {
            newItems[i] = VSOps.guard(fields[i], cond);
        }
        return new TupleVS(newItems);
    }

    @Override
    public TupleVS merge(TupleVS other) {
        final ValueSummary[] newItems = new ValueSummary[fields.length];
        for (int i = 0; i < fields.length; i++) {
            newItems[i] = VSOps.merge2(fields[i], other.fields[i]);
        }
        return new TupleVS(newItems);
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(TupleVS other, Bdd pc) {
        if (fields.length != other.fields.length) {
            return new PrimVS<>(false);
        }
        Bdd tupleEqual = IntStream.range(0, fields.length)
                .mapToObj((i) -> (Bdd) VSOps.symbolicEquals(fields[i], other.fields[i], pc).guardedValues.get(Boolean.TRUE))
                .reduce(Bdd::and)
                .orElse(Bdd.constTrue());
        return BoolUtils.fromTrueGuard(pc.and(tupleEqual));
    }
}
