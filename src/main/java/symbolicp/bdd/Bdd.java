package symbolicp.bdd;

import symbolicp.runtime.ScheduleLogger;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
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

    public static int trueQueries = 0;
    public static int falseQueries = 0;


    private static BddLib globalBddLib = new PjbddImpl();

    public static void reset() {
        trueQueries = 0; falseQueries = 0; globalBddLib = new PjbddImpl();
    }

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
        falseQueries++; return globalBddLib.isConstFalse(wrappedBdd);
    }

    public boolean isConstTrue() {
        trueQueries++; return globalBddLib.isConstTrue(wrappedBdd);
    }

    public Bdd and(Bdd other) {
        Instant start = Instant.now();
        Bdd res = new Bdd(globalBddLib.and(wrappedBdd, other.wrappedBdd));
        Instant end = Instant.now();
        /*
        if (Duration.between(start, end).toMillis() > 1000) {
            ScheduleLogger.log("Time taken for and: " + Duration.between(start, end).toMillis());
            String bddString = res.toString();
            int counter = 0;
            int index = bddString.indexOf("label");
            while(index >= 0) {
                index = bddString.indexOf("label", index+1);
                counter++;
            }
            ScheduleLogger.log(counter + " # BDD nodes");
        }

         */
        return res;
    }

    public Bdd or(Bdd other) {
        Instant start = Instant.now();
        Bdd res = new Bdd(globalBddLib.or(wrappedBdd, other.wrappedBdd));
        Instant end = Instant.now();
        /*
        if (Duration.between(start, end).toMillis() > 1000) {
            ScheduleLogger.log("Time taken for or: " + Duration.between(start, end).toMillis());
            String bddString = res.toString();
            int counter = 0;
            int index = bddString.indexOf("label");
            while (index >= 0) {
                index = bddString.indexOf("label", index + 1);
                counter++;
            }
            ScheduleLogger.log(counter + " # BDD nodes");
        }
         */
        return res;
    }

    public Bdd implies(Bdd other) { return new Bdd(globalBddLib.implies(wrappedBdd, other.wrappedBdd)); }

    public Bdd not() {
        Instant start = Instant.now();
        Bdd res = new Bdd(globalBddLib.not(wrappedBdd));
        Instant end = Instant.now();
        /*
        if (Duration.between(start, end).toMillis() > 1000) {
            ScheduleLogger.log("Time taken for not: " + Duration.between(start, end).toMillis());
            String bddString = res.toString();
            int counter = 0;
            int index = bddString.indexOf("label");
            while (index >= 0) {
                index = bddString.indexOf("label", index + 1);
                counter++;
            }
            ScheduleLogger.log(counter + " # BDD nodes");
        }
         */
        return res;
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
        return globalBddLib.toString(wrappedBdd);
    }
}
