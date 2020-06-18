package symbolicp.run;

import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.runtime.ReplayScheduler;
import symbolicp.runtime.Schedule;
import symbolicp.runtime.ScheduleLogger;
import symbolicp.runtime.Scheduler;

public class Assert {

    public static void prop(boolean p, Scheduler scheduler, Bdd pc) {
        prop(p, "", scheduler, pc);
    }

    public static void prop(boolean p, String msg, Scheduler scheduler, Bdd pc) {
        if (!p) {
            ScheduleLogger.log("Property violated: " + msg);
            throw new BugFoundException("Property violated: " + msg, pc);
        }
    }

}
