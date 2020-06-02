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
            newItems[i] = fields[i].guard(cond);
        }
        return new TupleVS(newItems);
    }

    @Override
    public TupleVS merge(TupleVS other) {
        final ValueSummary[] newItems = new ValueSummary[fields.length];
        for (int i = 0; i < fields.length; i++) {
            newItems[i] = this.fields[i].merge(other.fields[i]);
        }
        return new TupleVS(newItems);
    }

    @Deprecated
    public static class Ops implements ValueSummaryOps<TupleVS> {
        private final ValueSummaryOps[] fieldOps;

        public Ops(ValueSummaryOps... fieldOps) {
            this.fieldOps = fieldOps;
        }

        @Override
        public boolean isEmpty(TupleVS tupleVS) {
            // Optimization: Tuples should always be nonempty,
            // and all fields should exist under the same conditions
            return fieldOps[0].isEmpty(tupleVS.fields[0]);
        }

        @Override
        public TupleVS empty() {
            Object[] resultFields = new Object[fieldOps.length];
            for (int i = 0; i < fieldOps.length; i++) {
                resultFields[i] = fieldOps[i].empty();
            }
            return new TupleVS(resultFields);
        }

        @Override
        public TupleVS guard(TupleVS tupleVS, Bdd guard) {
            Object[] resultFields = new Object[fieldOps.length];
            for (int i = 0; i < fieldOps.length; i++) {
                resultFields[i] = fieldOps[i].guard(tupleVS.fields[i], guard);
            }
            return new TupleVS(resultFields);
        }

        @Override
        public TupleVS merge(Iterable<TupleVS> tuplesToMerge) {
            ArrayList[] resultFieldsToMerge = new ArrayList[fieldOps.length];

            for (int i = 0; i < fieldOps.length; i++) {
                resultFieldsToMerge[i] = new ArrayList();
            }

            for (TupleVS tupleVS : tuplesToMerge) {
                for (int i = 0; i < fieldOps.length; i++) {
                    resultFieldsToMerge[i].add(tupleVS.fields[i]);
                }
            }

            Object[] resultFields = new Object[fieldOps.length];
            for (int i = 0; i < fieldOps.length; i++) {
                resultFields[i] = fieldOps[i].merge(resultFieldsToMerge[i]);
            }

            return new TupleVS(resultFields);
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(TupleVS left, TupleVS right, Bdd pc) {
            if (left.fields.length != right.fields.length) {
                return new PrimVS<>(false);
            }
            Bdd tupleEqual = IntStream.range(0, left.fields.length)
                    .mapToObj((i) -> (Bdd) fieldOps[i].symbolicEquals(left.fields[i], right.fields[i], pc).guardedValues.get(Boolean.TRUE))
                    .reduce(Bdd::and)
                    .orElse(Bdd.constTrue());
            return BoolUtils.fromTrueGuard(pc.and(tupleEqual));
        }

        @Override
        public TupleVS merge2(TupleVS tupleVS1, TupleVS tupleVS2) {
            Object[] resultFields = new Object[fieldOps.length];
            for (int i = 0; i < fieldOps.length; i++) {
                resultFields[i] = fieldOps[i].merge2(tupleVS1.fields[i], tupleVS2.fields[i]);
            }
            return new TupleVS(resultFields);
        }
    }
}
