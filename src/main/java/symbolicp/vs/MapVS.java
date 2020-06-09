package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.security.KeyException;
import java.util.*;

/** Class for map value summaries */
public class MapVS<K, V extends ValueSummary<V>> implements ValueSummary<MapVS<K,V>> {
    /** The set of keys */
    public final SetVS<K> keys;
    /** The mapping from all possible keys to value summaries */
    public final Map<K, V> entries;

    /** Make a new MapVS with the specified set of keys and the specified mapping
     * @param keys The set of keys
     * @param entries The mapping from all possible keys to value summaries
     */
    public MapVS(SetVS<K> keys, Map<K, V> entries) {
        this.keys = keys;
        this.entries = entries;
    }

    /** Make a new MapVS with the specified universe
     * @param universe The universe for the new MapVS
     */
    public MapVS(Bdd universe) {
        this.keys = new SetVS<K>(universe);
        this.entries = new HashMap<>();
    }

    /** Get the number of entries in the MapVS
     * @return The size of the MapVS */
    public PrimVS<Integer> getSize() {
        return keys.size;
    }

    @Override
    public boolean isEmptyVS() {
        return keys.isEmptyVS();
    }

    @Override
    public MapVS<K, V> guard(Bdd guard) {
        final SetVS<K> newKeys = keys.guard(guard);
        final Map<K, V> newEntries = new HashMap<>();

        for (Map.Entry<K, V> entry : entries.entrySet()) {
            final V newValue = entry.getValue().guard(guard);
            if (!newValue.isEmptyVS()) {
                newEntries.put(entry.getKey(), newValue);
            }
        }
        return new MapVS<>(newKeys, newEntries);
    }

    @Override
    public MapVS<K, V> merge(Iterable<MapVS<K, V>> summaries) {
        final List<SetVS<K>> keysToMerge = new ArrayList<>();
        final Map<K, List<V>> valuesToMerge = new HashMap<>();

        // add this set of entries' values, too
        for (Map.Entry<K, V> entry : entries.entrySet()) {
            valuesToMerge
                    .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                    .add(entry.getValue());
        }

        for (MapVS<K, V> summary : summaries) {
            keysToMerge.add(summary.keys);

            for (Map.Entry<K, V> entry : summary.entries.entrySet()) {
                valuesToMerge
                        .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }

        final SetVS<K> mergedKeys = keys.merge(keysToMerge);

        final Map<K, V> mergedValues = new HashMap<>();
        for (Map.Entry<K, List<V>> entriesToMerge : valuesToMerge.entrySet()) {
            List<V> toMerge = entriesToMerge.getValue();
            if (toMerge.size() > 0) {
                mergedValues.put(entriesToMerge.getKey(), toMerge.get(0).merge(toMerge.subList(1, toMerge.size())));
            }
        }

        return new MapVS<>(mergedKeys, mergedValues);
    }

    @Override
    public MapVS<K, V> merge(MapVS<K, V> summary) {
        return merge(Collections.singletonList(summary));
    }

    @Override
    public MapVS<K, V> update(Bdd guard, MapVS<K, V> update) {
        return this.guard(guard.not()).merge(Collections.singletonList(update.guard(guard)));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(MapVS<K, V> cmp, Bdd pc) {
        Bdd equalCond = Bdd.constTrue();

        for (Map.Entry<K, Bdd> entry : this.keys.elements.entrySet()) {
            /* Check common k v pairs */
            if (cmp.keys.elements.containsKey(entry.getKey())) {
                Bdd presentAndEqual = (entry.getValue().and(cmp.keys.elements.get(entry.getKey())))
                        .and(this.entries.get(entry.getKey()).symbolicEquals(
                                cmp.entries.get(entry.getKey()), pc).getGuard(Boolean.TRUE));
                Bdd absent = entry.getValue().or(cmp.keys.elements.get(entry.getKey())).not();
                equalCond = absent.or(presentAndEqual).and(equalCond);
            }
            /* Keys unique to this must not be present*/
            else {
                equalCond = entry.getValue().not().and(equalCond);
            }
        }

        for (Map.Entry<K, Bdd> entry : cmp.keys.elements.entrySet()) {
            /* Keys unique to cmp must not be present*/
            if (!keys.elements.containsKey(entry.getKey())) {
                equalCond = entry.getValue().not().and(equalCond);
            }
        }
        return BoolUtils.fromTrueGuard(pc.and(equalCond));
    }

    @Override
    public Bdd getUniverse() {
        return keys.getUniverse();
    }

    /** Put a key-value pair into the MapVS
     * @param keySummary The key value summary
     * @param valSummary The value value summary
     * @return The updated MapVS
     */
    public MapVS<K, V> put(PrimVS<K> keySummary, V valSummary) {
        final SetVS<K> newKeys = keys.add(keySummary);
        final Map<K, V> newEntries = new HashMap<>(entries);
        for (GuardedValue<K> guardedKey : keySummary.getGuardedValues()) {
            V oldVal = entries.get(guardedKey.value);
            if (oldVal == null) {
                newEntries.put(guardedKey.value, valSummary);
            } else {
                newEntries.put(guardedKey.value, oldVal.update(guardedKey.guard, valSummary));
            }
        }

        return new MapVS<>(newKeys, newEntries);
    }

    /** Add a key-value pair into the MapVS
     * @param keySummary The key value summary
     * @param valSummary The value value summary
     * @return The updated MapVS
     */
    // TODO: Some parts of the non-symbolic P compiler and runtime seem to make a distinction
    //  between 'add' and 'put'.  Should we?
    public MapVS<K, V> add(PrimVS<K> keySummary, V valSummary) {
        return put(keySummary, valSummary);
    }

    /** Remove a key-value pair from the MapVS
     * @param keySummary The key value summary
     * @return The updated MapVS
     */
    public MapVS<K, V> remove(PrimVS<K> keySummary) {
        final SetVS<K> newKeys = keys.remove(keySummary);

        final Map<K, V> newEntries = new HashMap<>(entries);
        for (GuardedValue<K> guardedKey : keySummary.getGuardedValues()) {
            V oldVal = entries.get(guardedKey.value);
            if (oldVal == null) {
                continue;
            }

            final V remainingVal = oldVal.guard(guardedKey.guard.not());
            if (remainingVal.isEmptyVS()) {
                newEntries.remove(guardedKey.value);
            } else {
                newEntries.put(guardedKey.value, remainingVal);
            }
        }

        return new MapVS<>(newKeys, newEntries);
    }

    /** Get a value from from the MapVS
     * @param keySummary The key value summary.
     * @return The option containing value corresponding to the key or an empty option if no such value
     */
    public V get(PrimVS<K> keySummary) {
        final PrimVS<Boolean> containsKeySummary = keys.contains(keySummary);
        if (!containsKeySummary.getGuard(false).isConstFalse()) {
            // there is a possibility that the key is not present
            throw new NoSuchElementException();
        }

        V merger = null;
        List<V> toMerge = new ArrayList<>();
        for (K key : keySummary.getValues()) {
            if (merger == null)
                merger = entries.get(key);
            toMerge.add(entries.get(key));
        }

        return merger.merge(toMerge);
    }
}
