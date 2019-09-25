package symbolicp.prototypes;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PrimitiveValueSummary<Bdd, T> {
    /** The guards on these values *must* be mutually exclusive.
     *
     * In other words, for any two 'value1', 'value2' of type T, the following must be identically false:
     *
     *      and(guardedValues.get(value1), guardedValues.get(value2))
     *
     *  The map 'guardedValues' should never be modified.
     */
    public final Map<T, Bdd> guardedValues;

    public PrimitiveValueSummary(BddLib<Bdd> bddLib, T value) {
        this.guardedValues = Collections.singletonMap(value, bddLib.constTrue());
    }

    /** Caution: The caller must take care to ensure that the guards on the provided values are mutually exclusive.
     *
     * Additionally, the provided map should not be mutated after the object is constructed.
     */
    public PrimitiveValueSummary(Map<T, Bdd> guardedValues) {
        this.guardedValues = guardedValues;
    }

    public <U> PrimitiveValueSummary<Bdd, U> map(BddLib<Bdd> bddLib, Function<T, U> function) {
        final Map<U, Bdd> results = new HashMap<>();

        for (Map.Entry<T, Bdd> guardedValue : guardedValues.entrySet()) {
            final U mapped = function.apply(guardedValue.getKey());
            results.merge(mapped, guardedValue.getValue(), bddLib::or);
        }

        return new PrimitiveValueSummary<>(results);
    }

    public <U, V> PrimitiveValueSummary<Bdd, V>
    map2(PrimitiveValueSummary<Bdd, U> summary2, BddLib<Bdd> bddLib, BiFunction<T, U, V> function) {
        final Map<V, Bdd> results = new HashMap<>();

        for (Map.Entry<T, Bdd> val1 : this.guardedValues.entrySet()) {
            for (Map.Entry<U, Bdd> val2 : summary2.guardedValues.entrySet()) {
                final Bdd combinedGuard = bddLib.and(val1.getValue(), val2.getValue());
                if (bddLib.isConstFalse(combinedGuard)) {
                    continue;
                }

                final V mapped = function.apply(val1.getKey(), val2.getKey());
                results.merge(mapped, combinedGuard, bddLib::or);
            }
        }

        return new PrimitiveValueSummary<>(results);
    }

    public <Target>
    Target flatMap(
        ValueSummaryOps<Bdd, Target> ops,
        Function<T, Target> function
    ) {
        final List<Target> toMerge = new ArrayList<>();

        for (Map.Entry<T, Bdd> guardedValue : guardedValues.entrySet()) {
            final Target mapped = function.apply(guardedValue.getKey());
            toMerge.add(ops.guard(mapped, guardedValue.getValue()));
        }

        return ops.merge(toMerge);
    }

    public static class Ops<Bdd, T> implements ValueSummaryOps<Bdd, PrimitiveValueSummary<Bdd, T>> {
        final BddLib<Bdd> bddLib;

        public Ops(BddLib<Bdd> bddLib) {
            this.bddLib = bddLib;
        }

        @Override
        public boolean isEmpty(PrimitiveValueSummary<Bdd, T> summary) {
            return summary.guardedValues.isEmpty();
        }

        @Override
        public PrimitiveValueSummary<Bdd, T> empty() {
            return new PrimitiveValueSummary<>(new HashMap<>());
        }

        @Override
        public PrimitiveValueSummary<Bdd, T> guard(PrimitiveValueSummary<Bdd, T> summary, Bdd guard) {
            final Map<T, Bdd> result = new HashMap<>();

            for (Map.Entry<T, Bdd> entry : summary.guardedValues.entrySet()) {
                final Bdd newEntryGuard = bddLib.and(entry.getValue(), guard);
                if (!bddLib.isConstFalse(newEntryGuard)) {
                    result.put(entry.getKey(), newEntryGuard);
                }
            }

            return new PrimitiveValueSummary<>(result);
        }

        @Override
        public PrimitiveValueSummary<Bdd, T> merge(Iterable<PrimitiveValueSummary<Bdd, T>> summaries) {
            final Map<T, Bdd> result = new HashMap<>();

            for (PrimitiveValueSummary<Bdd, T> summary : summaries) {
                for (Map.Entry<T, Bdd> entry : summary.guardedValues.entrySet()) {
                    result.merge(entry.getKey(), entry.getValue(), bddLib::or);
                }
            }

            return new PrimitiveValueSummary<>(result);
        }

        public <Target>
        LazyValueSummary<Bdd, Target> flatMapLazy(
            ValueSummaryOps<Bdd, Target> targetOps,
            PrimitiveValueSummary<Bdd, T> primitiveSummary,
            Function<T, LazyValueSummary<Bdd, Target>> function
        ) {
            return new LazyValueSummary<>((outerGuard) -> {
                final List<Target> toMerge = new ArrayList<>();

                for (Map.Entry<T, Bdd> guardedValue : primitiveSummary.guardedValues.entrySet()) {
                    final Bdd fullGuard = bddLib.and(outerGuard, guardedValue.getValue());
                    if (bddLib.isConstFalse(fullGuard)) {
                        continue;
                    }

                    final Target mappedGuarded = function.apply(guardedValue.getKey()).evalWithGuard.apply(fullGuard);

                    toMerge.add(mappedGuarded);
                }

                return targetOps.merge(toMerge);
            });
        }
    }
}
