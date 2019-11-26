package symbolicp.prototypes;

import java.util.*;

public class MapVS<K, V> {
    public final SetVS<K> keys;
    public final Map<K, V> entries;

    public MapVS(SetVS<K> keys, Map<K, V> entries) {
        this.keys = keys;
        this.entries = entries;
    }

    public PrimVS<Integer> getSize() {
        return keys.size;
    }

    public static class Ops<K, V> implements ValueSummaryOps<MapVS<K, V>> {
        private final SetVS.Ops<K> setOps;
        private final ValueSummaryOps<V> valueOps;

        public Ops(BddLib<Bdd> bddLib, ValueSummaryOps<V> valueOps) {
            this.setOps = new SetVS.Ops<>();
            this.valueOps = valueOps;
        }


        @Override
        public boolean isEmpty(MapVS<K, V> summary) {
            return setOps.isEmpty(summary.keys);
        }

        @Override
        public MapVS<K, V> empty() {
            return new MapVS<>(setOps.empty(), new HashMap<>());
        }

        @Override
        public MapVS<K, V> guard(MapVS<K, V> summary, Bdd guard) {
            final SetVS<K> newKeys = setOps.guard(summary.keys, guard);
            final Map<K, V> newEntries = new HashMap<>();

            for (Map.Entry<K, V> entry : summary.entries.entrySet()) {
                final V newValue = valueOps.guard(entry.getValue(), guard);
                if (!valueOps.isEmpty(newValue)) {
                    newEntries.put(entry.getKey(), newValue);
                }
            }

            return new MapVS<>(newKeys, newEntries);
        }

        @Override
        public MapVS<K, V> merge(Iterable<MapVS<K, V>> summaries) {
            final List<SetVS<K>> keysToMerge = new ArrayList<>();
            final Map<K, List<V>> valuesToMerge = new HashMap<>();

            for (MapVS<K, V> summary : summaries) {
                keysToMerge.add(summary.keys);

                for (Map.Entry<K, V> entry : summary.entries.entrySet()) {
                    valuesToMerge
                        .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                        .add(entry.getValue());
                }
            }

            final SetVS<K> mergedKeys = setOps.merge(keysToMerge);

            final Map<K, V> mergedValues = new HashMap<>();
            for (Map.Entry<K, List<V>> entriesToMerge : valuesToMerge.entrySet()) {
                mergedValues.put(entriesToMerge.getKey(), valueOps.merge(entriesToMerge.getValue()));
            }

            return new MapVS<>(mergedKeys, mergedValues);
        }

        public MapVS<K, V>
        put(MapVS<K, V> mapSummary, PrimVS<K> keySummary, V valSummary) {
            final SetVS<K> newKeys = setOps.add(mapSummary.keys, keySummary);

            final Map<K, V> newEntries = new HashMap<>(mapSummary.entries);
            for (Map.Entry<K, Bdd> guardedKey : keySummary.guardedValues.entrySet()) {
                V oldVal = mapSummary.entries.get(guardedKey.getKey());
                if (oldVal == null) {
                    oldVal = valueOps.empty();
                }

                final V guardedOldVal = valueOps.guard(oldVal, guardedKey.getValue().not());
                final V guardedNewVal = valueOps.guard(valSummary, guardedKey.getValue());
                newEntries.put(guardedKey.getKey(), valueOps.merge(Arrays.asList(guardedOldVal, guardedNewVal)));
            }

            return new MapVS<>(newKeys, newEntries);
        }

        // TODO: Some parts of the non-symbolic P compiler and runtime seem to make a distinction
        //  between 'add' and 'put'.  Should we?
        public MapVS<K, V>
        add(MapVS<K, V> mapSummary, PrimVS<K> keySummary, V valSummary) {
            return put(mapSummary, keySummary, valSummary);
        }

        public MapVS<K, V>
        remove(MapVS<K, V> mapSummary, PrimVS<K> keySummary) {
            final SetVS<K> newKeys = setOps.remove(mapSummary.keys, keySummary);

            final Map<K, V> newEntries = new HashMap<>(mapSummary.entries);
            for (Map.Entry<K, Bdd> guardedKey : keySummary.guardedValues.entrySet()) {
                V oldVal = mapSummary.entries.get(guardedKey.getKey());
                if (oldVal == null) {
                    continue;
                }

                final V remainingVal = valueOps.guard(oldVal, guardedKey.getValue().not());
                if (valueOps.isEmpty(remainingVal)) {
                    newEntries.remove(guardedKey.getKey());
                } else {
                    newEntries.put(guardedKey.getKey(), remainingVal);
                }
            }

            return new MapVS<>(newKeys, newEntries);
        }

        public OptionalVS<V>
        get(MapVS<K, V> mapSummary, PrimVS<K> keySummary) {
            final OptionalVS.Ops<V> resultOps = new OptionalVS.Ops<>(valueOps);

            final PrimVS<Boolean> containsKeySummary = setOps.contains(mapSummary.keys, keySummary);

            return containsKeySummary.flatMap(resultOps, (containsKey) -> {
                if (containsKey) {
                    return keySummary.flatMap(resultOps, (key) -> resultOps.makePresent(mapSummary.entries.get(key)));
                } else {
                    return resultOps.makeAbsent();
                }
            });
        }
    }
}
