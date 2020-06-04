package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.*;

public class MapVS<K, V extends ValueSummary<V>> implements ValueSummary<MapVS<K, V>>{
    public final SetVS<K> keys;
    public final Map<K, V> entries;

    public MapVS(SetVS<K> keys, Map<K, V> entries) {
        this.keys = keys;
        this.entries = entries;
    }

    public MapVS() {
        this.keys = new SetVS<>();
        this.entries = new HashMap<>();
    }

    public PrimVS<Integer> getSize() {
        return keys.size;
    }

    @Override
    public MapVS<K, V> guard(Bdd cond) {
        final SetVS<K> newKeys = VSOps.guard(keys, cond);
        final Map<K, V> newEntries = new HashMap<>();

        for (Map.Entry<K, V> entry : entries.entrySet()) {
            final V newValue = VSOps.guard(entry.getValue(), cond);
            if (!VSOps.isEmpty(newValue)) {
                newEntries.put(entry.getKey(), newValue);
            }
        }

        return new MapVS<>(newKeys, newEntries);
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(MapVS<K, V> other, Bdd pc) {
        Bdd equalCond = Bdd.constTrue();

        for (Map.Entry<K, Bdd> entry : keys.elements.entrySet()) {
            /* Check common k v pairs */
            if (other.keys.elements.containsKey(entry.getKey())) {
                Bdd presentAndEqual = (entry.getValue().and(other.keys.elements.get(entry.getKey())))
                        .and(VSOps.symbolicEquals(entries.get(entry.getKey()),
                                other.entries.get(entry.getKey()), pc).guardedValues.get(Boolean.TRUE));
                Bdd absent = entry.getValue().or(other.keys.elements.get(entry.getKey())).not();
                equalCond = absent.or(presentAndEqual).and(equalCond);
            }
            /* Keys unique to this must not be present*/
            else {
                equalCond = entry.getValue().not().and(equalCond);
            }
        }

        for (Map.Entry<K, Bdd> entry : other.keys.elements.entrySet()) {
            /* Keys unique to other must not be present*/
            if (!keys.elements.containsKey(entry.getKey())) {
                equalCond = entry.getValue().not().and(equalCond);
            }
        }
        return BoolUtils.fromTrueGuard(pc.and(equalCond));
    }

    @Override
    public MapVS<K, V> merge(MapVS<K, V> other) {
        final Map<K, List<V>> valuesToMerge = new HashMap<>();

        for (Map.Entry<K, V> entry : entries.entrySet()) {
            valuesToMerge
                    .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                    .add(entry.getValue());
        }
        for (Map.Entry<K, V> entry : other.entries.entrySet()) {
            valuesToMerge
                    .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                    .add(entry.getValue());
        }

        final SetVS<K> mergedKeys = keys.merge(other.keys);

        final Map<K, V> mergedValues = new HashMap<>();
        for (Map.Entry<K, List<V>> entriesToMerge : valuesToMerge.entrySet()) {
            mergedValues.put(entriesToMerge.getKey(), VSOps.merge(entriesToMerge.getValue()));
        }

        return new MapVS<>(mergedKeys, mergedValues);
    }
}
