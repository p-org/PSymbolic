package symbolicp.run;

import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.runtime.RuntimeLogger;
import symbolicp.runtime.Scheduler;

public class Assert {

    public static void prop(boolean p, Scheduler scheduler, Bdd pc) {
        prop(p, "", scheduler, pc);
    }

    public static void prop(boolean p, String msg, Scheduler scheduler, Bdd pc) {
        if (!p) {
            RuntimeLogger.enable();
            System.out.println("Property violated: " + msg);
            RuntimeLogger.property("Property violated: " + msg);
            throw new BugFoundException("Property violated: " + msg, pc);
        }
    }

}
