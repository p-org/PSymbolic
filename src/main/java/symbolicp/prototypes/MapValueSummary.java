package symbolicp.prototypes;

import java.util.*;

public class MapValueSummary<Bdd, K, V> {
    public final SetValueSummary<Bdd, K> keys;
    public final Map<K, V> entries;

    public MapValueSummary(SetValueSummary<Bdd, K> keys, Map<K, V> entries) {
        this.keys = keys;
        this.entries = entries;
    }

    public PrimitiveValueSummary<Bdd, Integer> getSize() {
        return keys.size;
    }

    public static class Ops<Bdd, K, V> implements ValueSummaryOps<Bdd, MapValueSummary<Bdd, K, V>> {
        private final BddLib<Bdd> bddLib;
        private final SetValueSummary.Ops<Bdd, K> setOps;
        private final ValueSummaryOps<Bdd, V> valueOps;

        public Ops(BddLib<Bdd> bddLib, ValueSummaryOps<Bdd, V> valueOps) {
            this.bddLib = bddLib;
            this.setOps = new SetValueSummary.Ops<>(bddLib);
            this.valueOps = valueOps;
        }


        @Override
        public boolean isEmpty(MapValueSummary<Bdd, K, V> summary) {
            return setOps.isEmpty(summary.keys);
        }

        @Override
        public MapValueSummary<Bdd, K, V> empty() {
            return new MapValueSummary<>(setOps.empty(), new HashMap<>());
        }

        @Override
        public MapValueSummary<Bdd, K, V> guard(MapValueSummary<Bdd, K, V> summary, Bdd guard) {
            final SetValueSummary<Bdd, K> newKeys = setOps.guard(summary.keys, guard);
            final Map<K, V> newEntries = new HashMap<>();

            for (Map.Entry<K, V> entry : summary.entries.entrySet()) {
                final V newValue = valueOps.guard(entry.getValue(), guard);
                if (!valueOps.isEmpty(newValue)) {
                    newEntries.put(entry.getKey(), newValue);
                }
            }

            return new MapValueSummary<>(newKeys, newEntries);
        }

        @Override
        public MapValueSummary<Bdd, K, V> merge(Iterable<MapValueSummary<Bdd, K, V>> summaries) {
            final List<SetValueSummary<Bdd, K>> keysToMerge = new ArrayList<>();
            final Map<K, List<V>> valuesToMerge = new HashMap<>();

            for (MapValueSummary<Bdd, K, V> summary : summaries) {
                keysToMerge.add(summary.keys);

                for (Map.Entry<K, V> entry : summary.entries.entrySet()) {
                    valuesToMerge
                        .computeIfAbsent(entry.getKey(), (key) -> new ArrayList<>())
                        .add(entry.getValue());
                }
            }

            final SetValueSummary<Bdd, K> mergedKeys = setOps.merge(keysToMerge);

            final Map<K, V> mergedValues = new HashMap<>();
            for (Map.Entry<K, List<V>> entriesToMerge : valuesToMerge.entrySet()) {
                mergedValues.put(entriesToMerge.getKey(), valueOps.merge(entriesToMerge.getValue()));
            }

            return new MapValueSummary<>(mergedKeys, mergedValues);
        }

        public MapValueSummary<Bdd, K, V>
        put(MapValueSummary<Bdd, K, V> mapSummary, PrimitiveValueSummary<Bdd, K> keySummary, V valSummary) {
            final SetValueSummary<Bdd, K> newKeys = setOps.add(mapSummary.keys, keySummary);

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

            return new MapValueSummary<>(newKeys, newEntries);
        }
    }
}
