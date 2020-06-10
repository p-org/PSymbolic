package symbolicp.bdd;

import java.util.List;

/** This class implements different checks for invariants on Bdds */
public class Checks {

    /** Do the provided Bdds implement a disjoint union?
     * @param bdds The Bdds */
    public static boolean disjointUnion(Iterable<Bdd> bdds) {
        Bdd acc = Bdd.constFalse();
        for (Bdd bdd : bdds) {
            if (!acc.and(bdd).isConstFalse())
                return false;
            acc.or(bdd);
        }
        return true;
    }

    /** Do the provided lists of Bdds have the same universe?
     * @param a The first Bdd list
     * @param b The second Bdd list */
    public static boolean sameUniverse(List<Bdd> a, List<Bdd> b) {
       return sameUniverse(a, b);
    }

    /** Are the provided Bdds the same universe?
     * @param a The first Bdd
     * @param b The second Bdd */
    public static boolean sameUniverse(Bdd a, Bdd b) {
        return a.implies(b).isConstTrue() && b.implies(a).isConstTrue();
    }

}