package symbolicp.prototypes;

import java.util.*;

public class MapVS<Bdd, K, V> {
    public final SetVS<Bdd, K> keys;
    public final Map<K, V> entries;

    public MapVS(SetVS<Bdd, K> keys, Map<K, V> entries) {
        this.keys = keys;
        this.entries = entries;
    }

    public PrimVS<Bdd, Integer> getSize() {
        return keys.size;
    }

    public static class Ops<Bdd, K, V> implements ValueSummaryOps<Bdd, MapVS<Bdd, K, V>> {
        private final BddLib<Bdd> bddLib;
        private final SetVS.Ops<Bdd, K> setOps;
        private final ValueSummaryOps<Bdd, V> valueOps;

        public Ops(BddLib<Bdd> bddLib, ValueSummaryOps<Bdd, V> valueOps) {
            this.bddLib = bddLib;
            this.setOps = new SetVS.Ops<>(bddLib);
            this.valueOps = valueOps;
        }


        @Override
        public boolean isEmpty(MapVS<Bdd, K, V> summary) {
            return setOps.isEmpty(summary.keys);
        }

        @Override
        public MapVS<Bdd, K, V> empty() {
            return new MapVS<>(setOps.empty(), new HashMap<>());
        }

        @Override
        public MapVS<Bdd, K, V> guard(MapVS<Bdd, K, V> summary, Bdd guard) {
            final SetVS<Bdd, K> newKeys = setOps.guard(summary.keys, guard);
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
        public MapVS<Bdd, K, V> merge(Iterable<MapVS<Bdd, K, V>> summaries) {
            final List<SetVS<Bdd, K>> keysToMerge = new ArrayList<>();
            final Map<K, List<V>> valuesToMerge = new HashMap<>();

            for (MapVS<Bdd, K, V> summary : summaries) {
                keysToMerge.add(summary.keys);

                for (Map.Entry<K, V> entry : summary.entries.entrySet()) {
                    valuesToMerge
                        .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                        .add(entry.getValue());
                }
            }

            final SetVS<Bdd, K> mergedKeys = setOps.merge(keysToMerge);

            final Map<K, V> mergedValues = new HashMap<>();
            for (Map.Entry<K, List<V>> entriesToMerge : valuesToMerge.entrySet()) {
                mergedValues.put(entriesToMerge.getKey(), valueOps.merge(entriesToMerge.getValue()));
            }

            return new MapVS<>(mergedKeys, mergedValues);
        }

        public MapVS<Bdd, K, V>
        put(MapVS<Bdd, K, V> mapSummary, PrimVS<Bdd, K> keySummary, V valSummary) {
            final SetVS<Bdd, K> newKeys = setOps.add(mapSummary.keys, keySummary);

            final Map<K, V> newEntries = new HashMap<>(mapSummary.entries);
            for (Map.Entry<K, Bdd> guardedKey : keySummary.guardedValues.entrySet()) {
                V oldVal = mapSummary.entries.get(guardedKey.getKey());
                if (oldVal == null) {
                    oldVal = valueOps.empty();
                }

                final V guardedOldVal = valueOps.guard(oldVal, bddLib.not(guardedKey.getValue()));
                final V guardedNewVal = valueOps.guard(valSummary, guardedKey.getValue());
                newEntries.put(guardedKey.getKey(), valueOps.merge(Arrays.asList(guardedOldVal, guardedNewVal)));
            }

            return new MapVS<>(newKeys, newEntries);
        }

        public MapVS<Bdd, K, V>
        remove(MapVS<Bdd, K, V> mapSummary, PrimVS<Bdd, K> keySummary) {
            final SetVS<Bdd, K> newKeys = setOps.remove(mapSummary.keys, keySummary);

            final Map<K, V> newEntries = new HashMap<>(mapSummary.entries);
            for (Map.Entry<K, Bdd> guardedKey : keySummary.guardedValues.entrySet()) {
                V oldVal = mapSummary.entries.get(guardedKey.getKey());
                if (oldVal == null) {
                    continue;
                }

                final V remainingVal = valueOps.guard(oldVal, bddLib.not(guardedKey.getValue()));
                if (valueOps.isEmpty(remainingVal)) {
                    newEntries.remove(guardedKey.getKey());
                } else {
                    newEntries.put(guardedKey.getKey(), remainingVal);
                }
            }

            return new MapVS<>(newKeys, newEntries);
        }

        public OptionalVS<Bdd, V>
        get(MapVS<Bdd, K, V> mapSummary, PrimVS<Bdd, K> keySummary) {
            final OptionalVS.Ops<Bdd, V> resultOps = new OptionalVS.Ops<>(bddLib, valueOps);

            final PrimVS<Bdd, Boolean> containsKeySummary = setOps.contains(mapSummary.keys, keySummary);

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
