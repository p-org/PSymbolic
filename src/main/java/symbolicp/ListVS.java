package symbolicp;

import java.util.*;
import java.util.stream.IntStream;

public class ListVS<Item> {
    private final PrimVS<Integer> size;
    private final List<Item> items;

    private ListVS(PrimVS<Integer> size, List<Item> items) {
        this.size = size;
        this.items = items;
    }

    public ListVS() {
        this(new PrimVS<>(0), new ArrayList<>());
    }

    public static class Ops<Item> implements ValueSummaryOps<ListVS<Item>> {
        private final PrimVS.Ops<Integer> sizeOps;
        private final ValueSummaryOps<Item> itemOps;

        public Ops(ValueSummaryOps<Item> itemOps) {
            this.sizeOps = new PrimVS.Ops<>();
            this.itemOps = itemOps;
        }

        @Override
        public boolean isEmpty(ListVS<Item> summary) {
            return sizeOps.isEmpty(summary.size);
        }

        @Override
        public ListVS<Item> empty() {
            return new ListVS<>(sizeOps.empty(), new ArrayList<>());
        }

        @Override
        public ListVS<Item> guard(ListVS<Item> summary, Bdd guard) {
            final PrimVS<Integer> newSize = sizeOps.guard(summary.size, guard);
            final List<Item> newItems = new ArrayList<>();

            for (Item item : summary.items) {
                Item newItem = itemOps.guard(item, guard);
                if (itemOps.isEmpty(newItem)) {
                    break; // No items after this item are possible either due to monotonicity / non-sparseness
                }
                newItems.add(newItem);
            }

            return new ListVS<>(newSize, newItems);
        }

        @Override
        public ListVS<Item> merge(Iterable<ListVS<Item>> summaries) {
            final List<PrimVS<Integer>> sizesToMerge = new ArrayList<>();
            final List<List<Item>> itemsToMergeByIndex = new ArrayList<>();

            for (ListVS<Item> summary : summaries) {
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

            final PrimVS<Integer> mergedSize = sizeOps.merge(sizesToMerge);

            final List<Item> mergedItems = new ArrayList<>();

            for (List<Item> itemsToMerge : itemsToMergeByIndex) {
                final Item mergedItem = itemOps.merge(itemsToMerge);
                mergedItems.add(mergedItem);
            }

            return new ListVS<>(mergedSize, mergedItems);
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(ListVS<Item> left, ListVS<Item> right, Bdd pc) {
            Bdd equalCond = Bdd.constFalse();
            for (Map.Entry<Integer, Bdd> size : left.size.guardedValues.entrySet()) {
                if (right.size.guardedValues.containsKey(size.getKey())) {
                    Bdd listEqual = IntStream.range(0, size.getKey())
                            .mapToObj((i) -> itemOps.symbolicEquals(left.items.get(i), right.items.get(i), pc).guardedValues.get(Boolean.TRUE))
                            .reduce(Bdd::and)
                            .orElse(Bdd.constTrue());
                    equalCond = equalCond.or(listEqual);
                }
            }
            return BoolUtils.fromTrueGuard(pc.and(equalCond));
        }

        // origList and item should be possible under the same conditions.
        public ListVS<Item> add(ListVS<Item> origList, Item item) {
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

            return new ListVS<>(new PrimVS<>(newSizeValues), newItems);
        }

        // listSummary and indexSummary should be possible under the same conditions
        public OptionalVS<Item>
        get(ListVS<Item> listSummary, PrimVS<Integer> indexSummary) {
            final OptionalVS.Ops<Item> resultOps = new OptionalVS.Ops<>(itemOps);

            return indexSummary.flatMap(
                resultOps,
                (index) -> {
                    final PrimVS<Boolean> inRange =
                        listSummary.size.map((size) -> index < size);

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
        public OptionalVS<ListVS<Item>>
        set(ListVS<Item> origList, PrimVS<Integer> indexSummary, Item itemToSet) {
            final OptionalVS.Ops<ListVS<Item>> resultOps =
                new OptionalVS.Ops<>(this);

            final PrimVS<Boolean> inRange =
                origList.size.map2(indexSummary, (size, index) -> index < size);

            return inRange.flatMap(
                resultOps,
                (isInRange) -> {
                    if (isInRange) {
                        /* The actual computation happens in here, as if we had no error handling */

                        final List<Item> newItems = new ArrayList<>(origList.items);

                        for (Map.Entry<Integer, Bdd> guardedIndex : indexSummary.guardedValues.entrySet()) {
                            final int index = guardedIndex.getKey();
                            final Bdd guard = guardedIndex.getValue();

                            final Item origContribution = itemOps.guard(newItems.get(index), guard.not());
                            final Item newContribution = itemOps.guard(itemToSet, guard);
                            newItems.set(index, itemOps.merge(Arrays.asList(origContribution, newContribution)));
                        }

                        return resultOps.makePresent(new ListVS<>(origList.size, newItems));
                    } else {
                        return resultOps.makeAbsent();
                    }
                });
        }

        /* TODO 'contains' */

        public OptionalVS<ListVS<Item>>
        insert(ListVS<Item> origList, PrimVS<Integer> indexSummary, Item itemToInsert) {
            final OptionalVS.Ops<ListVS<Item>> resultOps =
                new OptionalVS.Ops<>(this);

            final PrimVS<Boolean> inRange =
                origList.size.map2(indexSummary, (size, index) -> index <= size);

            return inRange.flatMap(
                resultOps,
                (isInRange) -> {
                    if (isInRange) {
                        /* The actual computation happens in here, as if we had no error handling */

                        final PrimVS<Integer> newSize = origList.size.map((origSize) -> origSize + 1);

                        final List<Item> newItems = new ArrayList<>();

                        final int maxNewSize = origList.items.size() + 1;
                        for (int i = 0; i < maxNewSize; i++) {
                            final int index = i; // Placate lambda mutability rule

                            final PrimVS<Integer> insertionPointComparison =
                                indexSummary.map((insertionPoint) -> {
                                    if (index < insertionPoint) {
                                        return -1;
                                    } else if (index == insertionPoint) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                });

                            final Item newItem =
                                insertionPointComparison.flatMap(
                                    itemOps,
                                    (comparision) -> {
                                        if (comparision == -1) {
                                            return origList.items.get(index);
                                        } else if (comparision == 0) {
                                            return itemToInsert;
                                        } else {
                                            return origList.items.get(index - 1);
                                        }
                                    });

                            assert index == newItems.size();
                            newItems.add(newItem);
                        }

                        return resultOps.makePresent(new ListVS<>(newSize, newItems));
                    } else {
                        return resultOps.makeAbsent();
                    }
                }
            );
        }

        public OptionalVS<ListVS<Item>>
        removeAt(ListVS<Item> origList, PrimVS<Integer> indexSummary) {
            final OptionalVS.Ops<ListVS<Item>> resultOps =
                new OptionalVS.Ops<>(this);

            final PrimVS<Boolean> inRange =
                origList.size.map2(indexSummary, (size, index) -> index < size);

            return inRange.flatMap(
                resultOps,
                (isInRange) -> {
                    if (isInRange) {
                        /* The actual computation happens in here, as if we had no error handling. */

                        final PrimVS<Integer> newSize =
                            origList.size.map((size) -> size + 1);

                        final List<Item> newItems = new ArrayList<>();

                        final int maxNewSize = origList.items.size() - 1;
                        for (int i = 0; i < maxNewSize; i++) {
                            final int index = i; // Placate lambda mutability rule

                            final PrimVS<Boolean> afterRemovePoint =
                                indexSummary.map((removePoint) -> index >= removePoint);

                            final Item newItem =
                                afterRemovePoint.flatMap(
                                    itemOps,
                                    (isAfterRemovePoint) -> {
                                        if (isAfterRemovePoint) {
                                            return origList.items.get(index + 1);
                                        } else {
                                            return origList.items.get(index);
                                        }
                                    });

                            assert index == newItems.size();
                            newItems.add(newItem);
                        }

                        return resultOps.makePresent(new ListVS<>(newSize, newItems));
                    } else {
                        return resultOps.makeAbsent();
                    }
                }
            );
        }
    }
}
