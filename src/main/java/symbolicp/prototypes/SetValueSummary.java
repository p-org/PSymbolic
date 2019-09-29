package symbolicp.prototypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetValueSummary<Bdd, T> {
    /* Invariant: 'size' should be consistent with 'elements', in the sense that for any assignment of concrete Bdd
     * variables, the concrete entry in 'size' whose guard is satisfied, if such an entry exists, should match the
     * number of entries in 'elements' whose guards are satisfied.
     */
    public final PrimitiveValueSummary<Bdd, Integer> size;

    /* A key with no entry in 'elements' represents an element whose guard is identically false.
     * We should always keep 'elements' in a normalized state where no element has a guard which is identically false.
     */
    private final Map<T, Bdd> elements;

    /* Caution: Callers must take care to ensure that the above invariants are satisfied. */
    public SetValueSummary(PrimitiveValueSummary<Bdd, Integer> size, Map<T, Bdd> elements) {
        this.size = size;
        this.elements = elements;
    }

    public static class Ops<Bdd, T> implements ValueSummaryOps<Bdd, SetValueSummary<Bdd, T>> {
        private final BddLib<Bdd> bddLib;
        private final PrimitiveValueSummary.Ops<Bdd, Integer> sizeOps;

        public Ops(BddLib<Bdd> bddLib) {
            this.bddLib = bddLib;
            this.sizeOps = new PrimitiveValueSummary.Ops<>(bddLib);
        }

        @Override
        public boolean isEmpty(SetValueSummary<Bdd, T> summary) {
            return sizeOps.isEmpty(summary.size);
        }

        @Override
        public SetValueSummary<Bdd, T> empty() {
            return new SetValueSummary<>(sizeOps.empty(), new HashMap<>());
        }

        @Override
        public SetValueSummary<Bdd, T> guard(SetValueSummary<Bdd, T> summary, Bdd guard) {
            final PrimitiveValueSummary<Bdd, Integer> newSize = sizeOps.guard(summary.size, guard);

            final Map<T, Bdd> newElements = new HashMap<>();
            for (Map.Entry<T, Bdd> entry : summary.elements.entrySet()) {
                final Bdd newGuard = bddLib.and(entry.getValue(), guard);

                if (bddLib.isConstFalse(newGuard)) {
                    continue;
                }

                newElements.put(entry.getKey(), newGuard);
            }

            return new SetValueSummary<>(newSize, newElements);
        }

        @Override
        public SetValueSummary<Bdd, T> merge(Iterable<SetValueSummary<Bdd, T>> summaries) {
            List<PrimitiveValueSummary<Bdd, Integer>> sizesToMerge = new ArrayList<>();
            final Map<T, Bdd> mergedElements = new HashMap<>();

            for (SetValueSummary<Bdd, T> summary : summaries) {
                sizesToMerge.add(summary.size);

                for (Map.Entry<T, Bdd> entry : summary.elements.entrySet()) {
                    mergedElements.merge(entry.getKey(), entry.getValue(), bddLib::or);
                }
            }

            final PrimitiveValueSummary<Bdd, Integer> mergedSize = sizeOps.merge(sizesToMerge);

            return new SetValueSummary<>(mergedSize, mergedElements);
        }

        public PrimitiveValueSummary<Bdd, Boolean>
        contains(SetValueSummary<Bdd, T> setSummary, PrimitiveValueSummary<Bdd, T> itemSummary) {
            return itemSummary.flatMap(
                new PrimitiveValueSummary.Ops<>(bddLib),
                (item) -> {
                    Bdd itemGuard = setSummary.elements.get(item);
                    if (itemGuard == null) {
                        itemGuard = bddLib.constFalse();
                    }

                    return BoolUtils.fromTrueGuard(bddLib, itemGuard);
                });
        }

        public SetValueSummary<Bdd, T>
        add(SetValueSummary<Bdd, T> setSummary, PrimitiveValueSummary<Bdd, T> itemSummary) {
            final PrimitiveValueSummary<Bdd, Integer> newSize = setSummary.size.map(bddLib, (sizeVal) -> sizeVal + 1);

            final Map<T, Bdd> newElements = new HashMap<>(setSummary.elements);
            for (Map.Entry<T, Bdd> entry : itemSummary.guardedValues.entrySet()) {
                newElements.merge(entry.getKey(), entry.getValue(), bddLib::or);
            }

            return new SetValueSummary<>(newSize, newElements);
        }

        public SetValueSummary<Bdd, T>
        remove(SetValueSummary<Bdd, T> setSummary, PrimitiveValueSummary<Bdd, T> itemSummary) {
            final PrimitiveValueSummary<Bdd, Integer> newSize =
                setSummary.size.map2(
                    contains(setSummary, itemSummary),
                    bddLib,
                    (oldSize, alreadyContains) -> alreadyContains ? oldSize - 1 : oldSize
                );

            final Map<T, Bdd> newElements = new HashMap<>(setSummary.elements);
            for (Map.Entry<T, Bdd> entry : itemSummary.guardedValues.entrySet()) {
                final Bdd oldGuard = setSummary.elements.get(entry.getKey());
                if (oldGuard == null) {
                    continue;
                }

                final Bdd newGuard = bddLib.and(oldGuard, bddLib.not(entry.getValue()));
                newElements.put(entry.getKey(), newGuard);
            }

            return new SetValueSummary<>(newSize, newElements);
        }
    }
}
