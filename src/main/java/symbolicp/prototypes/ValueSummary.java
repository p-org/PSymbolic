package symbolicp.prototypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ValueSummary<Bdd, T> {
    private final BddLib<Bdd> bddLib;
    private final Map<T, Bdd> guardedValues;

    public BddLib<Bdd> getBddLib() {
        return bddLib;
    }

    public ValueSummary(BddLib<Bdd> bddLib, T value) {
        this.bddLib = bddLib;
        this.guardedValues = Collections.singletonMap(value, bddLib.constTrue());
    }

    private ValueSummary(BddLib<Bdd> bddLib, Map<T, Bdd> guardedValues) {
        this.bddLib = bddLib;
        this.guardedValues = guardedValues;
    }

    public <U> ValueSummary<Bdd, U> flatMap(Function<T, ValueSummary<Bdd, U>> function) {
        final Map<U, Bdd> results = new HashMap<>();

        for (Map.Entry<T, Bdd> origGuardedValue : guardedValues.entrySet()) {
            final ValueSummary<Bdd, U> mapped = function.apply(origGuardedValue.getKey());

            assert mapped.bddLib.equals(bddLib);

            for (Map.Entry<U, Bdd> mappedGuarded : mapped.guardedValues.entrySet()) {
                final Bdd fullGuard = bddLib.and(origGuardedValue.getValue(), mappedGuarded.getValue());

                if (bddLib.isConstFalse(fullGuard)) {
                    continue;
                }

                results.merge(mappedGuarded.getKey(), fullGuard, bddLib::or);
            }
        }

        return new ValueSummary<>(bddLib, results);
    }
}
