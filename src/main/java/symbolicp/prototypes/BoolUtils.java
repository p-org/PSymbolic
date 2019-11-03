package symbolicp.prototypes;

import java.util.HashMap;
import java.util.Map;

public class BoolUtils {
    private BoolUtils() {}

    public static <Bdd> PrimVS<Bdd, Boolean>
    fromTrueGuard(BddLib<Bdd> bddLib, Bdd guard) {
        if (bddLib.isConstFalse(guard)) {
            return new PrimVS<>(bddLib, false);
        }

        if (bddLib.isConstTrue(guard)) {
            return new PrimVS<>(bddLib, true);
        }

        final Map<Boolean, Bdd> entries = new HashMap<>();
        entries.put(true, guard);
        entries.put(false, bddLib.not(guard));
        return new PrimVS<>(entries);
    }
}
