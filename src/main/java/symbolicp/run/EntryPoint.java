package symbolicp.run;
import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.runtime.ReplayScheduler;
import symbolicp.runtime.Scheduler;
import symbolicp.*;

public class EntryPoint {

    public static void run(Program p, int depth) {
        Scheduler scheduler = new Scheduler();
        p.setScheduler(scheduler);
        int step = 0;
        scheduler.setErrorDepth(depth);
        try {
            scheduler.doSearch(p.getStart());
        } catch (BugFoundException e) {
            Bdd pc = e.pathConstraint;
            ReplayScheduler replay = new ReplayScheduler(scheduler.getSchedule(), pc);
            p.setScheduler(replay);
            replay.doSearch(scheduler.getStartMachine());
            System.exit(2);
        }
    }

    public static void main(String [] args) {
        run(new elevator(), 13);
    }

}
