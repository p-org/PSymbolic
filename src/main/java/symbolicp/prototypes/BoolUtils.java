package symbolicp.prototypes;

import java.util.HashMap;
import java.util.Map;

public final class BoolUtils {
    private BoolUtils() {}

    public static PrimVS<Boolean>
    fromTrueGuard(Bdd guard) {
        if (guard.isConstFalse()) {
            return new PrimVS<>(false);
        }

        if (guard.isConstTrue()) {
            return new PrimVS<>(true);
        }

        final Map<Boolean, Bdd> entries = new HashMap<>();
        entries.put(true, guard);
        entries.put(false, guard.not());
        return new PrimVS<>(entries);
    }
}
