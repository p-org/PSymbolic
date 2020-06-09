package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.*;
import java.util.stream.IntStream;

/** Class for list value summaries */
public class ListVS<T extends ValueSummary<T>> implements ValueSummary<ListVS<T>> {
    /** The size of the list */
    private final PrimVS<Integer> size;
    /** The contents of the list */
    private final List<T> items;

    private ListVS(PrimVS<Integer> size, List<T> items) {
        this.size = size;
        this.items = items;
    }

    /** Make a new ListVS with the specified universe
     * @param universe The universe for the new ListVS
     */
    public ListVS(Bdd universe) {
        this(new PrimVS<>(0).guard(universe), new ArrayList<>());
    }

    /** Copy-constructor for ListVS
     * @param old The ListVS to copy
     */
    public ListVS(ListVS<T> old) {
        this(new PrimVS<>(old.size), new ArrayList<>(old.items));
    }

    @Override
    public boolean isEmptyVS() {
        return size.isEmptyVS();
    }

    @Override
    public ListVS<T> guard(Bdd guard) {
        final PrimVS<Integer> newSize = size.guard(guard);
        final List<T> newItems = new ArrayList<>();

        for (T item : this.items) {
            T newItem = item.guard(guard);
            if (newItem.isEmptyVS()) {
                break; // No items after this item are possible either due to monotonicity / non-sparseness
            }
            newItems.add(newItem);
        }

        return new ListVS<>(newSize, newItems);
    }

    @Override
    public ListVS<T> update(Bdd guard, ListVS<T> update) {
        return this.guard(guard.not()).merge(Collections.singletonList(update.guard(guard)));
    }

    @Override
    public ListVS<T> merge(Iterable<ListVS<T>> summaries) {
        final List<PrimVS<Integer>> sizesToMerge = new ArrayList<>();
        final List<List<T>> itemsToMergeByIndex = new ArrayList<>();

        // first add this list's items to the itemsToMergeByIndex
        for (T item : this.items) {
            itemsToMergeByIndex.add(new ArrayList<>(Collections.singletonList(item)));
        }

        for (ListVS<T> summary : summaries) {
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

        final PrimVS<Integer> mergedSize = size.merge(sizesToMerge);

        final List<T> mergedItems = new ArrayList<>();

        for (List<T> itemsToMerge : itemsToMergeByIndex) {
            // TODO: cleanup later
            final T mergedItem = itemsToMerge.get(0).merge(itemsToMerge.subList(1, itemsToMerge.size()));
            mergedItems.add(mergedItem);
        }

        return new ListVS<>(mergedSize, mergedItems);
    }

    @Override
    public ListVS<T> merge(ListVS<T> summary) {
        return merge(Collections.singletonList(summary));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(ListVS<T> cmp, Bdd pc) {
        if (size.isEmptyVS()) {
            if (cmp.isEmptyVS()) {
                return BoolUtils.fromTrueGuard(pc);
            } else {
                return BoolUtils.fromTrueGuard(Bdd.constFalse());
            }
        }

        Bdd equalCond = Bdd.constFalse();
        for (GuardedValue<Integer> size : this.size.getGuardedValues()) {
            if (cmp.size.hasValue(size.value)) {
                Bdd listEqual = IntStream.range(0, size.value)
                        .mapToObj((i) -> this.items.get(i).symbolicEquals(cmp.items.get(i), pc).getGuard(Boolean.TRUE))
                        .reduce(Bdd::and)
                        .orElse(Bdd.constTrue());
                equalCond = equalCond.or(listEqual);
            }
        }
        return BoolUtils.fromTrueGuard(pc.and(equalCond));
    }

    @Override
    public Bdd getUniverse() {
        return size.getUniverse();
    }

    /** Add an item to the ListVS
     * @param item The Item to add to the ListVS. Should be possible under the same conditions as the ListVS.
     */
    public ListVS<T> add(T item) {
        // TODO: same conditions check
        final Map<Integer, Bdd> newSizeValues = new HashMap<>();
        final List<T> newItems = new ArrayList<>(this.items);

        for (GuardedValue<Integer> possibleSize : this.size.getGuardedValues()) {
            final int sizeValue = possibleSize.value;
            newSizeValues.put(sizeValue + 1, possibleSize.guard);

            final T guardedItemToAdd = item.guard(possibleSize.guard);
            if (sizeValue == newItems.size()) {
                newItems.add(guardedItemToAdd);
            } else {
                newItems.set(sizeValue, guardedItemToAdd.merge(newItems.get(sizeValue)));
            }
        }

        return new ListVS<>(new PrimVS<>(newSizeValues), newItems);
    }

    /** Is an index in range?
     * @param indexSummary The index to check
     */
    public PrimVS<Boolean> inRange(PrimVS<Integer> indexSummary) {
        return BoolUtils.and(IntUtils.lessThan(indexSummary, size),
                IntUtils.lessThan(-1, indexSummary));
    }

    /** Is an index in range?
     * @param index The index to check
     */
    public PrimVS<Boolean> inRange(int index) {
        return BoolUtils.and(IntUtils.lessThan(index, size), -1 < index);
    }

    /** Get an item from the ListVS
     * @param indexSummary The index to take from the ListVS. Should be possible under the same conditions as the ListVS.
     */
    public T get(PrimVS<Integer> indexSummary) {
        final PrimVS<Boolean> inRange = inRange(indexSummary);
        // make sure it is always in range
        if (!inRange.getGuard(false).isConstFalse()) {
            // there is a possibility that the index is out-of-bounds
            throw new IndexOutOfBoundsException();
        }

        T merger = null;
        List<T> toMerge = new ArrayList<>();
        // for each possible index value
        for (GuardedValue<Integer> index : indexSummary.getGuardedValues()) {
            T item = items.get(index.value).guard(index.guard);
            if (merger == null)
                merger = item;
            else
                toMerge.add(item);
        }

        return merger.merge(toMerge);
    }

    /** Set an item in the ListVS
     * @param indexSummary The index to set in the ListVS. Should be possible under the same conditions as the ListVS.
     * @param itemToSet The item to put in the ListVS. Should be possible under the same conditions as the ListVS
     * @return The result of setting the ListVS
     */
    public ListVS<T> set(PrimVS<Integer> indexSummary, T itemToSet) {
        final PrimVS<Boolean> inRange = inRange(indexSummary);
        // make sure it is always in range
        if (!inRange.getGuard(false).isConstFalse()) {
            // there is a possibility that the index is out-of-bounds
            throw new IndexOutOfBoundsException();
        }

        ListVS<T> merger = null;
        List<ListVS<T>> toMerge = new ArrayList<>();
        // for each possible index value
        for (GuardedValue<Integer> index : indexSummary.getGuardedValues()) {
            final List<T> newItems = new ArrayList<>(items);
            // the original item is updated when this is the index (i.e., index.guard holds)
            final T newEntry = newItems.get(index.value).update(index.guard, itemToSet);
            newItems.set(index.value, newEntry);
            ListVS<T> newList = new ListVS<>(size, newItems);
            if (merger == null)
                merger = newList;
            else
                toMerge.add(newList);
        }

        return merger.merge(toMerge);
    }

    /** Check whether the ListVS contains an element
     * @param element The element to check for. Should be possible under the same conditions as the ListVS.
     * @return Whether or not the ListVS contains an element
     */
    public PrimVS<Boolean> contains(T element) {
        PrimVS<Integer> i = new PrimVS<>(0).guard(getUniverse());

        PrimVS<Boolean> contains = new PrimVS<>(false).guard(getUniverse());

        while (!BoolUtils.isFalse(IntUtils.lessThan(i, size)))  {
            contains = BoolUtils.or(contains, element.symbolicEquals(get(i), getUniverse()));
            i = IntUtils.add(i, 1);
        }

        return contains;
    }


    /** Insert an item in the ListVS. Inserting at the end will produce an IndexOutOfBoundsException.
     * @param indexSummary The index to insert at in the ListVS. Should be possible under the same conditions as the ListVS.
     * @param itemToInsert The item to put in the ListVS. Should be possible under the same conditions as the ListVS
     * @return The result of inserting into the ListVS
     */
    public ListVS<T> insert(PrimVS<Integer> indexSummary, T itemToInsert) {
        final PrimVS<Boolean> inRange = inRange(indexSummary);
        // make sure it is always in range
        if (!inRange.getGuard(false).isConstFalse()) {
            // there is a possibility that the index is out-of-bounds
            throw new IndexOutOfBoundsException();
        }

        ListVS<T> merger = null;
        List<ListVS<T>> toMerge = new ArrayList<>();
        for (GuardedValue<Integer> index : indexSummary.getGuardedValues()) {
            // 1. add a new entry (we'll re-add the last entry)
            ListVS<T> newList = new ListVS<>(this);
            newList = newList.add(newList.get(IntUtils.subtract(size, 1)));

            // 2. setting at the insertion index
            PrimVS<Integer> current = indexSummary;
            T prev = newList.get(current);
            newList = newList.set(indexSummary, itemToInsert);
            current = IntUtils.add(current, 1);

            // 3. setting everything after insertion index to be the previous element
            // (but we can skip the very last one, since we've already added it)
            while (!BoolUtils.isFalse(IntUtils.lessThan(current, IntUtils.subtract(size, 1)))) {
                T old = newList.get(current);
                T update = old.update(BoolUtils.trueCond(IntUtils.lessThan(current, size)), prev);
                newList = newList.set(current, update);
                prev = old;
                current = IntUtils.add(current, 1);
            }

            if (merger == null)
                merger = newList;
            else
                toMerge.add(newList);
        }

        return merger.merge(toMerge);
    }

    /** Remove an item from the ListVS.
     * @param indexSummary The index to insert at in the ListVS. Should be possible under the same conditions as the ListVS.
     * @return The result of removing from the ListVS
     */
    public ListVS<T> removeAt(PrimVS<Integer> indexSummary) {
        final PrimVS<Boolean> inRange = inRange(indexSummary);
        // make sure it is always in range
        if (!inRange.getGuard(false).isConstFalse()) {
            // there is a possibility that the index is out-of-bounds
            throw new IndexOutOfBoundsException();
        }

        ListVS<T> merger = null;
        List<ListVS<T>> toMerge = new ArrayList<>();
        for (GuardedValue<Integer> index : indexSummary.getGuardedValues()) {
            // want to update size if it is greater than 0
            Bdd updateSizeCond = BoolUtils.trueCond(IntUtils.lessThan(0, size));
            // new size
            PrimVS<Integer> newSize = size.update(updateSizeCond, IntUtils.subtract(size, 1));

            ListVS<T> newList = new ListVS<>(newSize, items.subList(0, items.size() - 1));
            PrimVS<Integer> current = indexSummary;

            // Setting everything after removal index to be the next element
            while (!BoolUtils.isFalse(IntUtils.lessThan(current, newSize))) {
                T old = get(current);
                T next = get(IntUtils.add(current, 1));
                T update = old.update(BoolUtils.trueCond(IntUtils.lessThan(current, size)), next);
                newList = newList.set(current, update);
                current = IntUtils.add(current, 1);
            }

            if (merger == null)
                merger = newList;
            else
                toMerge.add(newList);
        }

        return merger.merge(toMerge);
    }
}
