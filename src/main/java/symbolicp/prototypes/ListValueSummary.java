package symbolicp.prototypes;

import java.util.*;

public class ListValueSummary<Bdd, Item> {
    private final PrimitiveValueSummary<Bdd, Integer> size;
    private final List<Item> items;

    private ListValueSummary(PrimitiveValueSummary<Bdd, Integer> size, List<Item> items) {
        this.size = size;
        this.items = items;
    }

    public ListValueSummary(BddLib<Bdd> bddLib) {
        this(new PrimitiveValueSummary<>(bddLib, 0), new ArrayList<>());
    }

    public static class Ops<Bdd, Item> implements ValueSummaryOps<Bdd, ListValueSummary<Bdd, Item>> {
        private final BddLib<Bdd> bddLib;
        private final PrimitiveValueSummary.Ops<Bdd, Integer> sizeOps;
        private final ValueSummaryOps<Bdd, Item> itemOps;

        public Ops(BddLib<Bdd> bddLib, ValueSummaryOps<Bdd, Item> itemOps) {
            this.bddLib = bddLib;
            this.sizeOps = new PrimitiveValueSummary.Ops<>(bddLib);
            this.itemOps = itemOps;
        }

        @Override
        public boolean isEmpty(ListValueSummary<Bdd, Item> summary) {
            return sizeOps.isEmpty(summary.size);
        }

        @Override
        public ListValueSummary<Bdd, Item> empty() {
            return new ListValueSummary<>(sizeOps.empty(), new ArrayList<>());
        }

        @Override
        public ListValueSummary<Bdd, Item> guard(ListValueSummary<Bdd, Item> summary, Bdd guard) {
            final PrimitiveValueSummary<Bdd, Integer> newSize = sizeOps.guard(summary.size, guard);
            final List<Item> newItems = new ArrayList<>();

            for (Item item : summary.items) {
                Item newItem = itemOps.guard(item, guard);
                if (itemOps.isEmpty(newItem)) {
                    break; // No items after this item are possible either due to monotonicity / non-sparseness
                }
                newItems.add(newItem);
            }

            return new ListValueSummary<>(newSize, newItems);
        }

        @Override
        public ListValueSummary<Bdd, Item> merge(Iterable<ListValueSummary<Bdd, Item>> summaries) {
            final List<PrimitiveValueSummary<Bdd, Integer>> sizesToMerge = new ArrayList<>();
            final List<List<Item>> itemsToMergeByIndex = new ArrayList<>();

            for (ListValueSummary<Bdd, Item> summary : summaries) {
                sizesToMerge.add(summary.size);

                for (int i = 0; i < summary.items.size(); i++) {
                    if (i < itemsToMergeByIndex.size()) {
                        itemsToMergeByIndex.get(i).add(summary.items.get(i));
                    } else {
                        assert i == itemsToMergeByIndex.size();
                        itemsToMergeByIndex.add(new ArrayList<>(Collections.singletonList(summary.items.get(i))));
                    }
                }
            }

            final PrimitiveValueSummary<Bdd, Integer> mergedSize = sizeOps.merge(sizesToMerge);

            final List<Item> mergedItems = new ArrayList<>();

            for (List<Item> itemsToMerge : itemsToMergeByIndex) {
                final Item mergedItem = itemOps.merge(itemsToMerge);
                mergedItems.add(mergedItem);
            }

            return new ListValueSummary<>(mergedSize, mergedItems);
        }

        // origList and item should be possible under the same conditions.
        public ListValueSummary<Bdd, Item> add(ListValueSummary<Bdd, Item> origList, Item item) {
            final Map<Integer, Bdd> newSizeValues = new HashMap<>();
            final List<Item> newItems = new ArrayList<>(origList.items);

            for (Map.Entry<Integer, Bdd> possibleSize : origList.size.guardedValues.entrySet()) {
                final int sizeValue = possibleSize.getKey();
                newSizeValues.put(sizeValue + 1, possibleSize.getValue());

                final Item guardedItemToAdd = itemOps.guard(item, possibleSize.getValue());
                if (sizeValue == newItems.size()) {
                    newItems.add(guardedItemToAdd);
                } else {
                    newItems.set(
                        sizeValue,
                        itemOps.merge(Arrays.asList(newItems.get(sizeValue), guardedItemToAdd))
                    );
                }
            }

            return new ListValueSummary<>(new PrimitiveValueSummary<>(newSizeValues), newItems);
        }

        // listSummary and indexSummary should be possible under the same conditions
        public OptionalValueSummary<Bdd, Item>
        get(ListValueSummary<Bdd, Item> listSummary, PrimitiveValueSummary<Bdd, Integer> indexSummary) {
            final OptionalValueSummary.Ops<Bdd, Item> resultOps = new OptionalValueSummary.Ops<>(bddLib, itemOps);

            return indexSummary.flatMap(
                resultOps,
                (index) -> {
                    final PrimitiveValueSummary<Bdd, Boolean> inRange =
                        listSummary.size.map(bddLib, (size) -> index < size);

                    return inRange.flatMap(
                        resultOps,
                        (isInRange) -> {
                            if (isInRange) {
                                return resultOps.makePresent(listSummary.items.get(index));
                            } else {
                                return resultOps.makeAbsent();
                            }
                        });
                });
        }

        // all arguments should be possible under the same conditions
        public OptionalValueSummary<Bdd, ListValueSummary<Bdd, Item>>
        set(ListValueSummary<Bdd, Item> origList, PrimitiveValueSummary<Bdd, Integer> indexSummary, Item itemToSet) {
            final OptionalValueSummary.Ops<Bdd, ListValueSummary<Bdd, Item>> resultOps =
                new OptionalValueSummary.Ops<>(bddLib, this);

            final PrimitiveValueSummary<Bdd, Boolean> inRange =
                origList.size.map2(indexSummary, bddLib, (size, index) -> index < size);

            return inRange.flatMap(
                resultOps,
                (isInRange) -> {
                    if (isInRange) {
                        /* The actual computation happens in here, as if we had no error handling */

                        final List<Item> newItems = new ArrayList<>(origList.items);

                        for (Map.Entry<Integer, Bdd> guardedIndex : indexSummary.guardedValues.entrySet()) {
                            final int index = guardedIndex.getKey();
                            final Bdd guard = guardedIndex.getValue();

                            final Item origContribution = itemOps.guard(newItems.get(index), bddLib.not(guard));
                            final Item newContribution = itemOps.guard(itemToSet, guard);
                            newItems.set(index, itemOps.merge(Arrays.asList(origContribution, newContribution)));
                        }

                        return resultOps.makePresent(new ListValueSummary<>(origList.size, newItems));
                    } else {
                        return resultOps.makeAbsent();
                    }
                });
        }
    }
}
