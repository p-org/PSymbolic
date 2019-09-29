package symbolicp.prototypes;

import java.util.HashMap;
import java.util.Map;

public class BoolUtils {
    private BoolUtils() {}

    public static <Bdd> PrimitiveValueSummary<Bdd, Boolean>
    fromTrueGuard(BddLib<Bdd> bddLib, Bdd guard) {
        if (bddLib.isConstFalse(guard)) {
            return new PrimitiveValueSummary<>(bddLib, false);
        }

        if (bddLib.isConstTrue(guard)) {
            return new PrimitiveValueSummary<>(bddLib, true);
        }

        final Map<Boolean, Bdd> entries = new HashMap<>();
        entries.put(true, guard);
        entries.put(false, bddLib.not(guard));
        return new PrimitiveValueSummary<>(entries);
    }
}
