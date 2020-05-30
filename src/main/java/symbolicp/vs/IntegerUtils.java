package symbolicp.vs;

import symbolicp.bdd.Bdd;

public class IntegerUtils {
    public static PrimVS<Integer> add(PrimVS<Integer> a, PrimVS<Integer> b) {
        return a.apply2(b, (x, y) -> x + y);
    }

    public static PrimVS<Integer> add(PrimVS<Integer> a, int i) {
        return a.apply(x -> x + i);
    }

    public static PrimVS<Integer> subtract(PrimVS<Integer> a, PrimVS<Integer> b) {
        return a.apply2(b, (x, y) -> x - y);
    }

    public static PrimVS<Integer> subtract(PrimVS<Integer> a, int i) {
        return a.apply(x -> x - i);
    }

    public static PrimVS<Boolean> lessThan(PrimVS<Integer> a, PrimVS<Integer> b) {
        return a.apply2(b, (x, y) -> x < y);
    }

    public static PrimVS<Boolean> lessThan(int a, PrimVS<Integer> b) {
        return b.apply(x -> a < x);
    }

    public static PrimVS<Boolean> lessThan(PrimVS<Integer> a, int b) {
        return a.apply(x -> x < b);
    }

}
