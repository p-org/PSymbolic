package symbolicp.util;

import symbolicp.bdd.Bdd;
import symbolicp.runtime.Schedule;
import symbolicp.runtime.Scheduler;
import symbolicp.runtime.State;
import symbolicp.vs.BoolUtils;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummary;

import java.util.HashSet;
import java.util.List;

/** This class implements different checks for invariants on Bdds and ValueSummaries */
public class Checks {

    private static class CheckViolatedException extends RuntimeException {
        public CheckViolatedException(String msg) {
            super(msg);
        }
    }

    /** Do the provided Bdds implement a disjoint union?
     * @param bdds The Bdds */
    public static boolean disjointUnion(Iterable<Bdd> bdds) {
        Bdd acc = Bdd.constFalse();
        for (Bdd bdd : bdds) {
            if (!acc.and(bdd).isConstFalse())
                return false;
            acc = acc.or(bdd);
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

    /** Are the provided ValueSummaries equal under the given guard?
     * @param a The first ValueSummary
     * @param b The second ValueSummary
     * @param guard The guard
     * @return Whether or not they are equal under the given guard */
    public static boolean equalUnder(ValueSummary a, ValueSummary b, Bdd guard) {
        if (!a.getClass().equals(b.getClass())) return false;
        return !BoolUtils.isEverFalse(a.guard(guard).symbolicEquals(b.guard(guard), guard).guard(guard));
    }

    /** Is the provided Bdd inside the Scheduler's current universe?
     * @param bdd The Bdd
     * @return Whether or not the Bdd is included in the Scheduler's universe
     */
    public static boolean includedIn(Bdd bdd) {
        return bdd.implies(Scheduler.universe).isConstTrue();
    }

    /** Is the provided Bdd inside another?
     * @param a The Bdd
     * @param b The envlosing Bdd
     * @return Whether or not the Bdd is included in the other
     */
    public static boolean includedIn(Bdd a, Bdd b) {
        return a.implies(b).isConstTrue();
    }

    public static boolean noRepeats(List a) {
        boolean res = true;
        for (int i = 0; i < a.size(); i++) {
            Object itm = a.get(i);
            for (int j = 0; j < a.size(); j++) {
                if (i != j)
                    res = res && !(itm.equals(a.get(j)));
            }
        }
        return res;
    }

    /** Will print the provided string if the condition doesn't hold and throw an exception
     * @param msg What to print
     * @param cond What to assert
     */
    public static void check(String msg, boolean cond) {
        if (!cond) {
            throw new CheckViolatedException(msg);
        }
    }

}
