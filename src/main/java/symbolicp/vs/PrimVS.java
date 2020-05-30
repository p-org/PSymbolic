package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** Make an empty PrimVS */
    public PrimVS() { this(new HashMap<>()); }

    /** Get all the different possible guarded values */
    public Iterable<GuardedValue<T>> getGuardedValues() {
        return guardedValues.entrySet().stream()
                .map(x -> new GuardedValue(x.getKey(), x.getValue())).collect(Collectors.toList());
    }

    @Override
    public Bdd getUniverse() {
        return Bdd.orMany(guardedValues.values().stream().collect(Collectors.toList()));
    }

    /** Get whether or not the provided value is a possibility
     *
     * @param value The provided value
     * @return Whether or not the provided value is a possibility
     */
    public boolean hasValue(T value) {
        return guardedValues.containsKey(value);
    }

    /** Get the guard for a given value
     *
     * @param value The value for which the guard should be gotten
     * @return The guard for the provided value
     */
    public Bdd getGuard(T value) {
        return guardedValues.getOrDefault(value, Bdd.constFalse());
    }

    public <U> PrimVS<U> apply(Function<T, U> function) {
        final Map<U, Bdd> results = new HashMap<>();

        for (GuardedValue<T> guardedValue : getGuardedValues()) {
            final U mapped = function.apply(guardedValue.value);
            results.merge(mapped, guardedValue.guard, Bdd::or);
        }

        return new PrimVS<>(results);
    }

    public <U, V> PrimVS<V>
    apply2(PrimVS<U> summary2, BiFunction<T, U, V> function) {
        final Map<V, Bdd> results = new HashMap<>();

        for (GuardedValue<T> val1 : this.getGuardedValues()) {
            for (GuardedValue<U> val2: summary2.getGuardedValues()) {
                final Bdd combinedGuard = val1.guard.and(val2.guard);
                if (combinedGuard.isConstFalse()) {
                    continue;
                }

                final V mapped = function.apply(val1.value, val2.value);
                results.merge(mapped, combinedGuard, Bdd::or);
            }
        }

        return new PrimVS<>(results);
    }


    public <Target extends ValueSummary<Target>>
    Target applyVS(
        Target mergeWith,
        Function<T, Target> function
    ) {
        final List<Target> toMerge = new ArrayList<>();

        for (GuardedValue<T> guardedValue : getGuardedValues()) {
            final Target mapped = function.apply(guardedValue.value);
            toMerge.add(mapped.guard(guardedValue.guard));
        }

        return mergeWith.merge(toMerge);
    }

    @Override
    public boolean isEmpty() {
        return guardedValues.isEmpty();
    }

    @Override
    public PrimVS<T> guard(Bdd guard) {
        final Map<T, Bdd> result = new HashMap<>();

        for (Map.Entry<T, Bdd> entry : guardedValues.entrySet()) {
            final Bdd newEntryGuard = entry.getValue().and(guard);
            if (!newEntryGuard.isConstFalse()) {
                result.put(entry.getKey(), newEntryGuard);
            }
        }

        return new PrimVS<>(result);
    }

    @Override
    public PrimVS<T> update(Bdd guard, PrimVS<T> update) {
        return this.guard(guard.not()).merge(Collections.singletonList(update.guard(guard)));
    }

    @Override
    public PrimVS<T> merge(Iterable<PrimVS<T>> summaries) {
        final Map<T, Bdd> result = new HashMap<>(guardedValues);

        for (PrimVS<T> summary : summaries) {
            for (Map.Entry<T, Bdd> entry : summary.guardedValues.entrySet()) {
                result.merge(entry.getKey(), entry.getValue(), Bdd::or);
            }
        }

        return new PrimVS<>(result);
    }

    @Override
    public PrimVS<T> merge(PrimVS<T> summary) {
        return merge(Collections.singletonList(summary));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(PrimVS<T> cmp, Bdd pc) {
        Bdd equalCond = Bdd.constFalse();
        for (Map.Entry<T, Bdd> entry : this.guardedValues.entrySet()) {
            if (cmp.guardedValues.containsKey(entry.getKey())) {
                equalCond = equalCond.or(entry.getValue().and(cmp.guardedValues.get(entry.getKey())));
            }
        }
        return BoolUtils.fromTrueGuard(pc.and(equalCond));
    }

    @Override
    public String toString() {
        return guardedValues.toString();
    }
}
