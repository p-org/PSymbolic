package symbolicp;

import java.util.ArrayList;

public class TupleVS {
    private final Object[] fields;

    public TupleVS(Object... items) {
        this.fields = items;
    }

    public Object getField(int i) {
        return fields[i];
    }

    public TupleVS setField(int i, Object val) {
        final Object[] newItems = new Object[fields.length];
        System.arraycopy(fields, 0, newItems, 0, fields.length);
        newItems[i] = val;
        return new TupleVS(newItems);
    }

    public static class Ops implements ValueSummaryOps<TupleVS> {
        private final ValueSummaryOps[] fieldOps;

        public Ops(ValueSummaryOps[] fieldOps) {
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
        public TupleVS merge2(TupleVS tupleVS1, TupleVS tupleVS2) {
            Object[] resultFields = new Object[fieldOps.length];
            for (int i = 0; i < fieldOps.length; i++) {
                resultFields[i] = fieldOps[i].merge2(tupleVS1.fields[i], tupleVS2.fields[i]);
            }
            return new TupleVS(resultFields);
        }
    }
}
