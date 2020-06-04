package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PrimVS<T> implements ValueSummary<PrimVS<T>> {
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

    @Override
    public PrimVS<T> guard(Bdd cond) {
        final Map<T, Bdd> result = new HashMap<>();

        for (Map.Entry<T, Bdd> entry : guardedValues.entrySet()) {
            final Bdd newEntryGuard = entry.getValue().and(cond);
            if (!newEntryGuard.isConstFalse()) {
                result.put(entry.getKey(), newEntryGuard);
            }
        }

        return new PrimVS<>(result);
    }

    @Override
    public PrimVS<T> merge(PrimVS<T> other) {
        final Map<T, Bdd> result = new HashMap<>();

        for (Map.Entry<T, Bdd> entry : this.guardedValues.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(), Bdd::or);
        }

        for (Map.Entry<T, Bdd> entry : other.guardedValues.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(), Bdd::or);
        }

        return new PrimVS<>(result);
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

    @Deprecated
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

    public <Target extends ValueSummary<Target>>
    Target flatMapOps(
            Function<T, Target> function
    ) {
        final List<Target> toMerge = new ArrayList<>();

        for (Map.Entry<T, Bdd> guardedValue : guardedValues.entrySet()) {
            final Target mapped = function.apply(guardedValue.getKey());
            toMerge.add(VSOps.guard(mapped, guardedValue.getValue()));
        }

        return VSOps.merge(toMerge);
    }


    @Override
    public PrimVS<Boolean> symbolicEquals(PrimVS<T> other, Bdd pc) {
        Bdd equalCond = Bdd.constFalse();
        for (Map.Entry<T, Bdd> entry : guardedValues.entrySet()) {
            if (other.guardedValues.containsKey(entry.getKey())) {
                equalCond = equalCond.or(entry.getValue().and(other.guardedValues.get(entry.getKey())));
            }
        }
        return BoolUtils.fromTrueGuard(pc.and(equalCond));
    }

    @Override
    public String toString() {
        return guardedValues.toString();
    }
}
