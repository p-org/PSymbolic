package symbolicp.vs;

import org.jetbrains.annotations.Nullable;
import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;

import java.awt.event.ItemEvent;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class VSOps {

    /***
     * Common Value Summary Ops
     */

    public static boolean isEmpty(ValueSummary<?> o) {
        return o == null;
    }

    public static <VS extends ValueSummary<VS>> VS empty() {
        return null;
    }

    public static <VS extends ValueSummary<VS>> VS guard(VS o, Bdd guard) {
        if (o != null) {
            return o.guard(guard);
        }
        return null;
    }

    public static <VS extends ValueSummary<VS>> VS merge(Iterable<VS> iterable) {
        VS res = null;
        for (VS item : iterable) {
            if (item != null) {
                if (res == null) res = item;
                else res = res.merge(item);
            }
        }
        return res;
    }

    public static <VS extends ValueSummary<VS>> VS merge2(VS left, VS right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.merge(right);
    }

    public static <VSA extends ValueSummary<VSA>, VSB extends ValueSummary<VSB>> PrimVS<Boolean>
    symbolicEquals(VSA left, VSB right, Bdd pc) {
        PrimVS<Boolean> res;
        if (left == null) {
            if (right == null) res = new PrimVS<>(Boolean.TRUE);
            else res = new PrimVS<>(Boolean.FALSE);
        }
        else if (right == null || !((ParameterizedType) left.getClass().getGenericSuperclass()).getActualTypeArguments()[0]
                .equals(((ParameterizedType) right.getClass().getGenericSuperclass()).getActualTypeArguments()[0])){
            res = new PrimVS<>(Boolean.FALSE);
        }
        else {
            res = left.symbolicEquals((VSA) right, pc);
        }
        return res.guard(pc);
    }

    /***
     * Special Value Summary Ops
     */
    /* Set VS */
    public static <T> PrimVS<Boolean> contains(SetVS<T> setSummary, PrimVS<T> itemSummary) {
        return itemSummary.flatMapOps(
                (item) -> {
                    Bdd itemGuard = setSummary.elements.get(item);
                    if (itemGuard == null) {
                        itemGuard = Bdd.constFalse();
                    }

                    return BoolUtils.fromTrueGuard(itemGuard);
                });
    }

    public static <T> SetVS<T> add(SetVS<T> setSummary, PrimVS<T> itemSummary) {
        final PrimVS<Integer> newSize = setSummary.size.map((sizeVal) -> sizeVal + 1);

        final Map<T, Bdd> newElements = new HashMap<>(setSummary.elements);
        for (Map.Entry<T, Bdd> entry : itemSummary.guardedValues.entrySet()) {
            newElements.merge(entry.getKey(), entry.getValue(), Bdd::or);
        }

        return new SetVS<>(newSize, newElements);
    }

    public static <T> SetVS<T> remove(SetVS<T> setSummary, PrimVS<T> itemSummary) {
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

    /* Optional VS */
    public static <Item extends ValueSummary<Item>> Item unwrapOrThrow(OptionalVS<Item> summary) {
        final @Nullable Bdd absentCond = summary.present.guardedValues.get(false);
        if (absentCond != null && !absentCond.isConstFalse()) {
            throw new BugFoundException("Attempt to unwrap an absent optional value", absentCond);
        }

        return summary.item;
    }

    public static <Item extends ValueSummary<Item>> OptionalVS<Item> makePresent(Item item) {
        /* TODO: Strictly speaking, here we create an optional whose 'present' tag may violate
         *  an implicit invariant by being true under more conditions than we need it
         *  to be.
         */
        return new OptionalVS<>(
                new PrimVS<>(true),
                item
        );
    }

    public static <Item extends ValueSummary<Item>> OptionalVS<Item> makeAbsent() {
        /* TODO: Strictly speaking, here we create an optional whose 'present' tag may violate
         *  an implicit invariant by being false under more conditions than we need it
         *  to be.
         */
        return new OptionalVS<>(
                new PrimVS<>( false),
                null
        );
    }


    // FIXME: putting new entries do not update keys.size
    public static <K, V extends ValueSummary<V>> MapVS<K, V>
    put(MapVS<K, V> mapSummary, PrimVS<K> keySummary, V valSummary) {
        final SetVS<K> newKeys = add(mapSummary.keys, keySummary);

        final Map<K, V> newEntries = new HashMap<>(mapSummary.entries);
        for (Map.Entry<K, Bdd> guardedKey : keySummary.guardedValues.entrySet()) {
            V oldVal = mapSummary.entries.get(guardedKey.getKey());
            if (oldVal == null) {
                oldVal = empty();
            }

            final V guardedOldVal = guard(oldVal, guardedKey.getValue().not());
            final V guardedNewVal = guard(valSummary, guardedKey.getValue());
            newEntries.put(guardedKey.getKey(), merge(Arrays.asList(guardedOldVal, guardedNewVal)));
        }

        return new MapVS<>(newKeys, newEntries);
    }

    // TODO: Some parts of the non-symbolic P compiler and runtime seem to make a distinction
    //  between 'add' and 'put'.  Should we?
    public static <K, V extends ValueSummary<V>> MapVS<K, V>
    add(MapVS<K, V> mapSummary, PrimVS<K> keySummary, V valSummary) {
        return put(mapSummary, keySummary, valSummary);
    }

    public static <K, V extends ValueSummary<V>> MapVS<K, V>
    remove(MapVS<K, V> mapSummary, PrimVS<K> keySummary) {
        final SetVS<K> newKeys = remove(mapSummary.keys, keySummary);

        final Map<K, V> newEntries = new HashMap<>(mapSummary.entries);
        for (Map.Entry<K, Bdd> guardedKey : keySummary.guardedValues.entrySet()) {
            V oldVal = mapSummary.entries.get(guardedKey.getKey());
            if (oldVal == null) {
                continue;
            }

            final V remainingVal = guard(oldVal, guardedKey.getValue().not());
            if (isEmpty(remainingVal)) {
                newEntries.remove(guardedKey.getKey());
            } else {
                newEntries.put(guardedKey.getKey(), remainingVal);
            }
        }

        return new MapVS<>(newKeys, newEntries);
    }

    public static <K, V extends ValueSummary<V>> OptionalVS<V>
    get(MapVS<K, V> mapSummary, PrimVS<K> keySummary) {

        final PrimVS<Boolean> containsKeySummary = contains(mapSummary.keys, keySummary);

        return containsKeySummary.flatMapOps((containsKey) -> {
            if (containsKey) {
                return keySummary.flatMapOps((key) -> makePresent(mapSummary.entries.get(key)));
            } else {
                return makeAbsent();
            }
        });
    }

    /* ListVS */
    // origList and item should be possible under the same conditions.
    public static <Item extends ValueSummary<Item>> ListVS<Item>
    add(ListVS<Item> origList, Item item) {
        final Map<Integer, Bdd> newSizeValues = new HashMap<>();
        final List<Item> newItems = new ArrayList<>(origList.getItems());

        for (Map.Entry<Integer, Bdd> possibleSize : origList.getSize().guardedValues.entrySet()) {
            final int sizeValue = possibleSize.getKey();
            newSizeValues.put(sizeValue + 1, possibleSize.getValue());

            final Item guardedItemToAdd = guard(item, possibleSize.getValue());
            if (sizeValue == newItems.size()) {
                newItems.add(guardedItemToAdd);
            } else {
                newItems.set(
                        sizeValue,
                        merge(Arrays.asList(newItems.get(sizeValue), guardedItemToAdd))
                );
            }
        }

        return new ListVS<>(new PrimVS<>(newSizeValues), newItems);
    }

    // listSummary and indexSummary should be possible under the same conditions
    public static <Item extends ValueSummary<Item>> OptionalVS<Item>
    get(ListVS<Item> listSummary, PrimVS<Integer> indexSummary) {
        return indexSummary.flatMapOps(
                (index) -> {
                    final PrimVS<Boolean> inRange =
                            listSummary.getSize().map((size) -> index < size);

                    return inRange.flatMapOps(
                            (isInRange) -> {
                                if (isInRange) {
                                    return makePresent(listSummary.getItems().get(index));
                                } else {
                                    return makeAbsent();
                                }
                            });
                });
    }

    // all arguments should be possible under the same conditions
    public static <Item extends ValueSummary<Item>> OptionalVS<ListVS<Item>>
    set(ListVS<Item> origList, PrimVS<Integer> indexSummary, Item itemToSet) {
        final PrimVS<Boolean> inRange =
                origList.getSize().map2(indexSummary, (size, index) -> index < size);

        return inRange.flatMapOps(
                (isInRange) -> {
                    if (isInRange) {
                        /* The actual computation happens in here, as if we had no error handling */

                        final List<Item> newItems = new ArrayList<>(origList.getItems());

                        for (Map.Entry<Integer, Bdd> guardedIndex : indexSummary.guardedValues.entrySet()) {
                            final int index = guardedIndex.getKey();
                            final Bdd guard = guardedIndex.getValue();

                            final Item origContribution = guard(newItems.get(index), guard.not());
                            final Item newContribution = guard(itemToSet, guard);
                            newItems.set(index, merge(Arrays.asList(origContribution, newContribution)));
                        }

                        return makePresent(new ListVS<>(origList.getSize(), newItems));
                    } else {
                        return makeAbsent();
                    }
                });
    }

    /* TODO 'contains' */

    public static <Item extends ValueSummary<Item>> OptionalVS<ListVS<Item>>
    insert(ListVS<Item> origList, PrimVS<Integer> indexSummary, Item itemToInsert) {
        final PrimVS<Boolean> inRange =
                origList.getSize().map2(indexSummary, (size, index) -> index <= size);

        return inRange.flatMapOps(
                (isInRange) -> {
                    if (isInRange) {
                        /* The actual computation happens in here, as if we had no error handling */

                        final PrimVS<Integer> newSize = origList.getSize().map((origSize) -> origSize + 1);

                        final List<Item> newItems = new ArrayList<>();

                        final int maxNewSize = origList.getItems().size() + 1;
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
                                    insertionPointComparison.flatMapOps(
                                            (comparision) -> {
                                                if (comparision == -1) {
                                                    return origList.getItems().get(index);
                                                } else if (comparision == 0) {
                                                    return itemToInsert;
                                                } else {
                                                    return origList.getItems().get(index - 1);
                                                }
                                            });

                            assert index == newItems.size();
                            newItems.add(newItem);
                        }

                        return makePresent(new ListVS<>(newSize, newItems));
                    } else {
                        return makeAbsent();
                    }
                }
        );
    }

    public static <Item extends ValueSummary<Item>> OptionalVS<ListVS<Item>>
    removeAt(ListVS<Item> origList, PrimVS<Integer> indexSummary) {
        final PrimVS<Boolean> inRange =
                origList.getSize().map2(indexSummary, (size, index) -> index < size);

        return inRange.flatMapOps(
                (isInRange) -> {
                    if (isInRange) {
                        /* The actual computation happens in here, as if we had no error handling. */

                        final PrimVS<Integer> newSize =
                                origList.getSize().map((size) -> size - 1);

                        final List<Item> newItems = new ArrayList<>();

                        final int maxNewSize = origList.getItems().size() - 1;
                        for (int i = 0; i < maxNewSize; i++) {
                            final int index = i; // Placate lambda mutability rule

                            final PrimVS<Boolean> afterRemovePoint =
                                    indexSummary.map((removePoint) -> index >= removePoint);

                            final Item newItem =
                                    afterRemovePoint.flatMapOps(
                                            (isAfterRemovePoint) -> {
                                                if (isAfterRemovePoint) {
                                                    return origList.getItems().get(index + 1);
                                                } else {
                                                    return origList.getItems().get(index);
                                                }
                                            });

                            assert index == newItems.size();
                            newItems.add(newItem);
                        }

                        return makePresent(new ListVS<>(newSize, newItems));
                    } else {
                        return makeAbsent();
                    }
                }
        );
    }
}

