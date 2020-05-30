package symbolicp.vs;

import org.checkerframework.checker.units.qual.A;
import symbolicp.bdd.Bdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TupleVS implements ValueSummary<TupleVS> {
    private final ValueSummary[] fields;
    private final Class[] classes;

    public TupleVS(ValueSummary... items) {
        this.fields = items;
        this.classes = Arrays.asList(items).stream().map(x -> x.getClass())
                .collect(Collectors.toList()).toArray(new Class[items.length]);
    }

    public ValueSummary getField(int i) {
        return fields[i];
    }

    public TupleVS setField(int i, ValueSummary val) {
        final ValueSummary[] newItems = new ValueSummary[fields.length];
        System.arraycopy(fields, 0, newItems, 0, fields.length);
        if (!(val.getClass().equals(classes[i]))) throw new ClassCastException();
        newItems[i] = val;
        return new TupleVS(newItems);
    }

    @Override
    public boolean isEmpty() {
        // Optimization: Tuples should always be nonempty,
        // and all fields should exist under the same conditions
        return fields[0].isEmpty();
    }

    @Override
    public TupleVS guard(Bdd guard) {
        ValueSummary[] resultFields = new ValueSummary[fields.length];
        for (int i = 0; i < fields.length; i++) {
            resultFields[i] = fields[i].guard(guard);
        }
        return new TupleVS(resultFields);
    }

    @Override
    public TupleVS merge(Iterable<TupleVS> summaries) {
        List<ValueSummary> resultList = Arrays.asList(fields);
        for (TupleVS summary : summaries) {
            for (int i = 0; i < summary.fields.length; i++) {
                if (i < resultList.size()) {
                    resultList.set(i, resultList.get(i).merge(summary.fields[i]));
                } else {
                    resultList.add(summary.fields[i]);
                }
            }
        }
        return new TupleVS(resultList.toArray(new ValueSummary[resultList.size()]));
    }

    @Override
    public TupleVS merge(TupleVS summary) {
        return merge(Collections.singletonList(summary));
    }

    @Override
    public TupleVS update(Bdd guard, TupleVS update) {
        return this.guard(guard.not()).merge(Collections.singletonList(update.guard(guard)));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(TupleVS cmp, Bdd pc) {
        if (fields.length != cmp.fields.length) {
            return new PrimVS<>(false);
        }
        Bdd tupleEqual = IntStream.range(0, fields.length)
                .mapToObj((i) -> fields[i].symbolicEquals(cmp.fields[i], pc).getGuard(Boolean.TRUE))
                .reduce(Bdd::and)
                .orElse(Bdd.constTrue());
        return BoolUtils.fromTrueGuard(pc.and(tupleEqual));
    }

    @Override
    public Bdd getUniverse() {
        // Optimization: Tuples should always be nonempty,
        // and all fields should exist under the same conditions
        return fields[0].getUniverse();
    }
}
