package symbolicp.prototypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetVS<Bdd, T> {
    /* Invariant: 'size' should be consistent with 'elements', in the sense that for any assignment of concrete Bdd
     * variables, the concrete entry in 'size' whose guard is satisfied, if such an entry exists, should match the
     * number of entries in 'elements' whose guards are satisfied.
     */
    public final PrimVS<Bdd, Integer> size;

    /* A key with no entry in 'elements' represents an element whose guard is identically false.
     * We should always keep 'elements' in a normalized state where no element has a guard which is identically false.
     */
    private final Map<T, Bdd> elements;

    /* Caution: Callers must take care to ensure that the above invariants are satisfied. */
    public SetVS(PrimVS<Bdd, Integer> size, Map<T, Bdd> elements) {
        this.size = size;
        this.elements = elements;
    }

    public static class Ops<Bdd, T> implements ValueSummaryOps<Bdd, SetVS<Bdd, T>> {
        private final BddLib<Bdd> bddLib;
        private final PrimVS.Ops<Bdd, Integer> sizeOps;

        public Ops(BddLib<Bdd> bddLib) {
            this.bddLib = bddLib;
            this.sizeOps = new PrimVS.Ops<>(bddLib);
        }

        @Override
        public boolean isEmpty(SetVS<Bdd, T> summary) {
            return sizeOps.isEmpty(summary.size);
        }

        @Override
        public SetVS<Bdd, T> empty() {
            return new SetVS<>(sizeOps.empty(), new HashMap<>());
        }

        @Override
        public SetVS<Bdd, T> guard(SetVS<Bdd, T> summary, Bdd guard) {
            final PrimVS<Bdd, Integer> newSize = sizeOps.guard(summary.size, guard);

            final Map<T, Bdd> newElements = new HashMap<>();
            for (Map.Entry<T, Bdd> entry : summary.elements.entrySet()) {
                final Bdd newGuard = bddLib.and(entry.getValue(), guard);

                if (bddLib.isConstFalse(newGuard)) {
                    continue;
                }

                newElements.put(entry.getKey(), newGuard);
            }

            return new SetVS<>(newSize, newElements);
        }

        @Override
        public SetVS<Bdd, T> merge(Iterable<SetVS<Bdd, T>> summaries) {
            List<PrimVS<Bdd, Integer>> sizesToMerge = new ArrayList<>();
            final Map<T, Bdd> mergedElements = new HashMap<>();

            for (SetVS<Bdd, T> summary : summaries) {
                sizesToMerge.add(summary.size);

                for (Map.Entry<T, Bdd> entry : summary.elements.entrySet()) {
                    mergedElements.merge(entry.getKey(), entry.getValue(), bddLib::or);
                }
            }

            final PrimVS<Bdd, Integer> mergedSize = sizeOps.merge(sizesToMerge);

            return new SetVS<>(mergedSize, mergedElements);
        }

        public PrimVS<Bdd, Boolean>
        contains(SetVS<Bdd, T> setSummary, PrimVS<Bdd, T> itemSummary) {
            return itemSummary.flatMap(
                new PrimVS.Ops<>(bddLib),
                (item) -> {
                    Bdd itemGuard = setSummary.elements.get(item);
                    if (itemGuard == null) {
                        itemGuard = bddLib.constFalse();
                    }

                    return BoolUtils.fromTrueGuard(bddLib, itemGuard);
                });
        }

        public SetVS<Bdd, T>
        add(SetVS<Bdd, T> setSummary, PrimVS<Bdd, T> itemSummary) {
            final PrimVS<Bdd, Integer> newSize = setSummary.size.map(bddLib, (sizeVal) -> sizeVal + 1);

            final Map<T, Bdd> newElements = new HashMap<>(setSummary.elements);
            for (Map.Entry<T, Bdd> entry : itemSummary.guardedValues.entrySet()) {
                newElements.merge(entry.getKey(), entry.getValue(), bddLib::or);
            }

            return new SetVS<>(newSize, newElements);
        }

        public SetVS<Bdd, T>
        remove(SetVS<Bdd, T> setSummary, PrimVS<Bdd, T> itemSummary) {
            final PrimVS<Bdd, Integer> newSize =
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

            return new SetVS<>(newSize, newElements);
        }
    }
}
