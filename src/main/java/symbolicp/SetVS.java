package symbolicp;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetVS<T> {
    /* Invariant: 'size' should be consistent with 'elements', in the sense that for any assignment of concrete Bdd
     * variables, the concrete entry in 'size' whose guard is satisfied, if such an entry exists, should match the
     * number of entries in 'elements' whose guards are satisfied.
     */
    public final PrimVS<Integer> size;

    /* A key with no entry in 'elements' represents an element whose guard is identically false.
     * We should always keep 'elements' in a normalized state where no element has a guard which is identically false.
     */
    public final Map<T, Bdd> elements;

    /* Caution: Callers must take care to ensure that the above invariants are satisfied. */
    public SetVS(PrimVS<Integer> size, Map<T, Bdd> elements) {
        this.size = size;
        this.elements = elements;
    }

    public SetVS() {
        this.size = new PrimVS<>(0);
        this.elements = new HashMap<>();
    }

    public static class Ops<T> implements ValueSummaryOps<SetVS<T>> {
        private final PrimVS.Ops<Integer> sizeOps;

        public Ops() {
            this.sizeOps = new PrimVS.Ops<>();
        }

        @Override
        public boolean isEmpty(SetVS<T> summary) {
            return sizeOps.isEmpty(summary.size);
        }

        @Override
        public SetVS<T> empty() {
            return new SetVS<>(sizeOps.empty(), new HashMap<>());
        }

        @Override
        public SetVS<T> guard(SetVS<T> summary, Bdd guard) {
            final PrimVS<Integer> newSize = sizeOps.guard(summary.size, guard);

            final Map<T, Bdd> newElements = new HashMap<>();
            for (Map.Entry<T, Bdd> entry : summary.elements.entrySet()) {
                final Bdd newGuard = entry.getValue().and(guard);

                if (newGuard.isConstFalse()) {
                    continue;
                }

                newElements.put(entry.getKey(), newGuard);
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

            final PrimVS<Integer> mergedSize = sizeOps.merge(sizesToMerge);

            return new SetVS<>(mergedSize, mergedElements);
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(SetVS<T> left, SetVS<T> right, Bdd pc) {
            Bdd equalCond = Bdd.constTrue();
            for (Map.Entry<T, Bdd> entry : left.elements.entrySet()) {
                /* Check common elements */
                if (right.elements.containsKey(entry.getKey())) {
                    equalCond = (entry.getValue().and(right.elements.get(entry.getKey()))) //both present
                            .or(entry.getValue().or(right.elements.get(entry.getKey())).not()) //both not present
                            .and(equalCond);
                }
                /* Elements unique to left must not be present*/
                else {
                    equalCond = entry.getValue().not().and(equalCond);
                }
            }
            for (Map.Entry<T, Bdd> entry : right.elements.entrySet()) {
                /* Elements unique to right must not be present*/
                if (!right.elements.containsKey(entry.getKey())) {
                    equalCond = entry.getValue().not().and(equalCond);
                }
            }
            return BoolUtils.fromTrueGuard(pc.and(equalCond));
        }

        public PrimVS<Boolean>
        contains(SetVS<T> setSummary, PrimVS<T> itemSummary) {
            return itemSummary.flatMap(
                new PrimVS.Ops<>(),
                (item) -> {
                    Bdd itemGuard = setSummary.elements.get(item);
                    if (itemGuard == null) {
                        itemGuard = Bdd.constFalse();
                    }

                    return BoolUtils.fromTrueGuard(itemGuard);
                });
        }

        public SetVS<T>
        add(SetVS<T> setSummary, PrimVS<T> itemSummary) {
            final PrimVS<Integer> newSize = setSummary.size.map((sizeVal) -> sizeVal + 1);

            final Map<T, Bdd> newElements = new HashMap<>(setSummary.elements);
            for (Map.Entry<T, Bdd> entry : itemSummary.guardedValues.entrySet()) {
                newElements.merge(entry.getKey(), entry.getValue(), Bdd::or);
            }

            return new SetVS<>(newSize, newElements);
        }

        public SetVS<T>
        remove(SetVS<T> setSummary, PrimVS<T> itemSummary) {
            final PrimVS<Integer> newSize =
                setSummary.size.map2(
                    contains(setSummary, itemSummary),
                    (oldSize, alreadyContains) -> alreadyContains ? oldSize - 1 : oldSize
                );

            final Map<T, Bdd> newElements = new HashMap<>(setSummary.elements);
            for (Map.Entry<T, Bdd> entry : itemSummary.guardedValues.entrySet()) {
                final Bdd oldGuard = setSummary.elements.get(entry.getKey());
                if (oldGuard == null) {
                    continue;
                }

                final Bdd newGuard = oldGuard.and(entry.getValue().not());
                newElements.put(entry.getKey(), newGuard);
            }

            return new SetVS<>(newSize, newElements);
        }
    }
}
