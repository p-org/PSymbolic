package symbolicp.run;

import symbolicp.bdd.Bdd;
import symbolicp.runtime.ScheduleLogger;
import symbolicp.runtime.Scheduler;

public class Assert {

    public static void prop(boolean p, Bdd pc) {
        prop(p, "", pc);
    }

    public static void prop(boolean p, String msg, Bdd pc) {
        if (!p) {
            ScheduleLogger.log("Property violated: " + msg);
            ScheduleLogger.log(Scheduler.schedule.singleScheduleToString(pc));
            System.exit(2);
        }
    }

}
