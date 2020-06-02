package symbolicp.vs;

import symbolicp.bdd.Bdd;

public class VSOps {

    public static boolean isEmpty(ValueSummary<?> o) {
        return o == null;
    }

    public static <VS extends ValueSummary<VS>> VS empty() {
        return null;
    }

    public static <VS extends ValueSummary<VS>> VS guard(VS o, Bdd guard) {
        if (o != null) {
            return o.guard(guard);
        }
        return null;
    }

    public static <VS extends ValueSummary<VS>> VS merge(Iterable<VS> iterable) {
        VS res = null;
        for (VS item : iterable) {
            if (item != null) {
                res = item.merge(res);
            }
        }
        return res;
    }

    public static <VS extends ValueSummary<VS>> VS merge2(VS left, VS right) {
        if (left == null) {
            return right;
        }
        return left.merge(right);
    }

    public static <VS extends ValueSummary<VS>> PrimVS<Boolean> symbolicEquals(VS left, VS right, Bdd pc) {
        PrimVS<Boolean> res;
        if (left == null) {
            if (right == null) res = new PrimVS<>(Boolean.TRUE);
            else res = new PrimVS<>(Boolean.FALSE);
        }
        else {
            res = left.symbolicEquals(right, pc);
        }
        return res.guard(pc);
    }
}
