package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.*;
import java.util.stream.Collectors;

public class SetVS<T> implements ValueSummary<SetVS<T>> {
    /* Invariant: 'size' should be consistent with 'elements', in the sense that for any assignment of concrete Bdd
     * variables, the concrete entry in 'size' whose guard is satisfied, if such an entry exists, should match the
     * number of entries in 'elements' whose guards are satisfied.
     */
    public final PrimVS<Integer> size;

    /* A key with no entry in 'elements' represents an element whose guard is identically false.
     * We should always keep 'elements' in a normalized state where no element has a guard which is identically false.
     */
    public final Map<T, Bdd> elements;

    /** Get all the different possible guarded values */
    public Iterable<GuardedValue<T>> getElements() {
        return elements.entrySet().stream()
                .map(x -> new GuardedValue<T>(x.getKey(), x.getValue())).collect(Collectors.toList());
    }

    /* Caution: Callers must take care to ensure that the above invariants are satisfied. */
    public SetVS(PrimVS<Integer> size, Map<T, Bdd> elements) {
        this.size = size;
        this.elements = elements;
    }

    public SetVS(Bdd universe) {
        this.size = new PrimVS<>(0).guard(universe);
        this.elements = new HashMap<>();
    }

    @Override
    public boolean isEmpty() {
        return size.isEmpty();
    }

    @Override
    public SetVS<T> guard(Bdd guard) {
        final PrimVS<Integer> newSize = size.guard(guard);

        final Map<T, Bdd> newElements = new HashMap<>();
        for (GuardedValue<T> entry : getElements()) {
            final Bdd newGuard = entry.guard.and(guard);
            if (newGuard.isConstFalse()) {
                continue;
            }
            newElements.put(entry.value, newGuard);
        }

        return new SetVS<>(newSize, newElements);
    }

    @Override
    public SetVS<T> merge(Iterable<SetVS<T>> summaries) {
        List<PrimVS<Integer>> sizesToMerge = new ArrayList<>();
        final Map<T, Bdd> mergedElements = new HashMap<>();

        for (SetVS<T> summary : summaries) {
            sizesToMerge.add(summary.size);

            for (Map.Entry<T, Bdd> entry : summary.elements.entrySet()) {
                mergedElements.merge(entry.getKey(), entry.getValue(), Bdd::or);
            }
        }

        final PrimVS<Integer> mergedSize = size.merge(sizesToMerge);

        return new SetVS<>(mergedSize, mergedElements);
    }

    @Override
    public SetVS<T> merge(SetVS<T> summary) {
        return merge(Collections.singletonList(summary));
    }

    @Override
    public SetVS<T> update(Bdd guard, SetVS<T> update) {
        return this.guard(guard.not()).merge(Collections.singletonList(update.guard(guard)));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(SetVS<T> cmp, Bdd pc) {
        Bdd equalCond = Bdd.constTrue();
        for (Map.Entry<T, Bdd> entry : elements.entrySet()) {
            /* Check common elements */
            if (cmp.elements.containsKey(entry.getKey())) {
                equalCond = (entry.getValue().and(cmp.elements.get(entry.getKey()))) //both present
                        .or(entry.getValue().or(cmp.elements.get(entry.getKey())).not()) //both not present
                        .and(equalCond);
            }
            /* Elements unique to this must not be present*/
            else {
                equalCond = entry.getValue().not().and(equalCond);
            }
        }
        for (Map.Entry<T, Bdd> entry : cmp.elements.entrySet()) {
            /* Elements unique to cmp must not be present*/
            if (!cmp.elements.containsKey(entry.getKey())) {
                equalCond = entry.getValue().not().and(equalCond);
            }
        }
        return BoolUtils.fromTrueGuard(pc.and(equalCond));
    }

    @Override
    public Bdd getUniverse() {
        return size.getUniverse();
    }

    public PrimVS<Boolean> contains(PrimVS<T> itemSummary) {
        return itemSummary.applyVS(
                new PrimVS<>(),
                (item) -> {
                    Bdd itemGuard = elements.get(item);
                    if (itemGuard == null) {
                        itemGuard = Bdd.constFalse();
                    }

                    return BoolUtils.fromTrueGuard(itemGuard);
                });
    }

    public SetVS<T> add(PrimVS<T> itemSummary) {
        // Update size only when item exists
        final PrimVS<Integer> newSize = size.update(itemSummary.getUniverse(), IntegerUtils.add(size, 1));

        final Map<T, Bdd> newElements = new HashMap<>(elements);
        for (GuardedValue<T> entry : itemSummary.getGuardedValues()) {
            newElements.merge(entry.value, entry.guard, Bdd::or);
        }

        return new SetVS<>(newSize, newElements);
    }

    public SetVS<T> remove(PrimVS<T> itemSummary) {
        // Is the item contained?
        final PrimVS<Boolean> contained = contains(itemSummary);
        // Update size only when item contained
        final PrimVS<Integer> newSize = size.update(BoolUtils.trueCond(contained), IntegerUtils.subtract(size, 1));

        final Map<T, Bdd> newElements = new HashMap<>(elements);
        for (GuardedValue<T> entry : itemSummary.getGuardedValues()) {
            final Bdd oldGuard = elements.get(entry.value);
            if (oldGuard == null) {
                continue;
            }
            final Bdd newGuard = oldGuard.and(entry.guard.not());
            newElements.put(entry.value, newGuard);
        }

        return new SetVS<>(newSize, newElements);
    }
}
