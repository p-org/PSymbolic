package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.HashMap;
import java.util.Map;

public final class BoolUtils {
    private BoolUtils() {}

    public static PrimVS<Boolean> fromTrueGuard(Bdd guard) {
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

    public static Bdd trueCond(PrimVS<Boolean> primVS) {
        return primVS.getGuard(true);
    }

    public static Bdd falseCond(PrimVS<Boolean> primVS) {
        return primVS.getGuard(false);
    }

    public static PrimVS<Boolean> and(PrimVS<Boolean> a, PrimVS<Boolean> b) {
        return a.apply2(b, (x, y) -> x && y);
    }

    public static PrimVS<Boolean> and(PrimVS<Boolean> a, boolean b) {
        return a.apply(x -> x && b);
    }

    public static PrimVS<Boolean> and(boolean a, PrimVS<Boolean> b) {
        return and(b, a);
    }

    public static PrimVS<Boolean> or(PrimVS<Boolean> a, PrimVS<Boolean> b) {
        return a.apply2(b, (x, y) -> x || y);
    }

    public static boolean isFalse(PrimVS<Boolean> b) {
        return falseCond(b).isConstTrue();
    }
}
