package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PrimVS<T> {
    /** The guards on these values *must* be mutually exclusive.
     *
     * In other words, for any two 'value1', 'value2' of type T, the following must be identically false:
     *
     *      and(guardedValues.get(value1), guardedValues.get(value2))
     *
     *  The map 'guardedValues' should never be modified.
     */
    public final Map<T, Bdd> guardedValues;

    public PrimVS(T value) {
        this.guardedValues = Collections.singletonMap(value, Bdd.constTrue());
    }

    /** Caution: The caller must take care to ensure that the guards on the provided values are mutually exclusive.
     *
     * Additionally, the provided map should not be mutated after the object is constructed.
     */
    public PrimVS(Map<T, Bdd> guardedValues) {
        this.guardedValues = guardedValues;
    }

    public <U> PrimVS<U> map(Function<T, U> function) {
        final Map<U, Bdd> results = new HashMap<>();

        for (Map.Entry<T, Bdd> guardedValue : guardedValues.entrySet()) {
            final U mapped = function.apply(guardedValue.getKey());
            results.merge(mapped, guardedValue.getValue(), Bdd::or);
        }

        return new PrimVS<>(results);
    }

    public <U, V> PrimVS<V>
    map2(PrimVS<U> summary2, BiFunction<T, U, V> function) {
        final Map<V, Bdd> results = new HashMap<>();

        for (Map.Entry<T, Bdd> val1 : this.guardedValues.entrySet()) {
            for (Map.Entry<U, Bdd> val2 : summary2.guardedValues.entrySet()) {
                final Bdd combinedGuard = val1.getValue().and(val2.getValue());
                if (combinedGuard.isConstFalse()) {
                    continue;
                }

                final V mapped = function.apply(val1.getKey(), val2.getKey());
                results.merge(mapped, combinedGuard, Bdd::or);
            }
        }

        return new PrimVS<>(results);
    }

    public <Target>
    Target flatMap(
        ValueSummaryOps<Target> ops,
        Function<T, Target> function
    ) {
        final List<Target> toMerge = new ArrayList<>();

        for (Map.Entry<T, Bdd> guardedValue : guardedValues.entrySet()) {
            final Target mapped = function.apply(guardedValue.getKey());
            toMerge.add(ops.guard(mapped, guardedValue.getValue()));
        }

        return ops.merge(toMerge);
    }

    public static class Ops<T> implements ValueSummaryOps<PrimVS<T>> {
        public Ops() { }

        @Override
        public boolean isEmpty(PrimVS<T> summary) {
            return summary.guardedValues.isEmpty();
        }

        @Override
        public PrimVS<T> empty() {
            return new PrimVS<>(new HashMap<>());
        }

        @Override
        public PrimVS<T> guard(PrimVS<T> summary, Bdd guard) {
            final Map<T, Bdd> result = new HashMap<>();

            for (Map.Entry<T, Bdd> entry : summary.guardedValues.entrySet()) {
                final Bdd newEntryGuard = entry.getValue().and(guard);
                if (!newEntryGuard.isConstFalse()) {
                    result.put(entry.getKey(), newEntryGuard);
                }
            }

            return new PrimVS<>(result);
        }

        @Override
        public PrimVS<T> merge(Iterable<PrimVS<T>> summaries) {
            final Map<T, Bdd> result = new HashMap<>();

            for (PrimVS<T> summary : summaries) {
                for (Map.Entry<T, Bdd> entry : summary.guardedValues.entrySet()) {
                    result.merge(entry.getKey(), entry.getValue(), Bdd::or);
                }
            }

            return new PrimVS<>(result);
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(PrimVS<T> left, PrimVS<T> right, Bdd pc) {
            Bdd equalCond = Bdd.constFalse();
            for (Map.Entry<T, Bdd> entry : left.guardedValues.entrySet()) {
                if (right.guardedValues.containsKey(entry.getKey())) {
                    equalCond = equalCond.or(entry.getValue().and(right.guardedValues.get(entry.getKey())));
                }
            }
            return BoolUtils.fromTrueGuard(pc.and(equalCond));
        }
    }

    @Override
    public String toString() {
        return guardedValues.toString();
    }
}
