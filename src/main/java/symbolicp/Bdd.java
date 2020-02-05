package symbolicp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * This class determines the global BDD implementation used by the symbolic engine.
 *
 * It is a thin wrapper over a BddLib, which can be swapped out at will by reassigning the `globalBddLib` variable.
 *
 * The implementation of this class itself is relatively loosely typed, but both its clients and the BddLib it wraps can
 * enjoy a strongly-typed interface.
 *
 * This class may be a candidate for performance optimization in the future, if it turns out that wrapping every
 * Bdd inside an additional object represents a significant performance bottleneck.
 */
public final class Bdd {
    private static BddLib globalBddLib = new BDDSylvanImpl();

    private final Object wrappedBdd;

    /* Directly constructing Bdd wrappers is not advisable and should generally only be used for testing purposes */
    public Bdd(Object wrappedBdd) {
        this.wrappedBdd = wrappedBdd;
    }

    public static Bdd constFalse() {
        return new Bdd(globalBddLib.constFalse());
    }

    public static Bdd constTrue() {
        return new Bdd(globalBddLib.constTrue());
    }

    public boolean isConstFalse() {
        return globalBddLib.isConstFalse(wrappedBdd);
    }

    public boolean isConstTrue() {
        return globalBddLib.isConstTrue(wrappedBdd);
    }

    public Bdd and(Bdd other) {
        return new Bdd(globalBddLib.and(wrappedBdd, other.wrappedBdd));
    }

    public Bdd or(Bdd other) {
        return new Bdd(globalBddLib.or(wrappedBdd, other.wrappedBdd));
    }

    public Bdd not() {
        return new Bdd(globalBddLib.not(wrappedBdd));
    }

    public static Bdd orMany(List<Bdd> wrappedBdd) {
        return wrappedBdd.stream().reduce(Bdd.constFalse(), Bdd::or);
    }

    public Bdd ifThenElse(Bdd thenCase, Bdd elseCase) {
        return new Bdd(globalBddLib.ifThenElse(wrappedBdd, thenCase.wrappedBdd, elseCase.wrappedBdd));
    }

    public static Bdd newVar() {
        return new Bdd(globalBddLib.newVar());
    }

    @Override
    public String toString() {
        return wrappedBdd.toString();
    }
}
