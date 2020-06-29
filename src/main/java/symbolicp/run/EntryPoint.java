package symbolicp.run;
import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;
import symbolicp.runtime.*;
import symbolicp.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntryPoint {

    public static void run(Program p, int depth) {
        Scheduler scheduler = new Scheduler();
        p.setScheduler(scheduler);
        int step = 0;
        scheduler.setErrorDepth(depth);
        try {
            scheduler.doSearch(p.getStart());
            ScheduleLogger.enable();
            ScheduleLogger.finished(scheduler.getDepth());
        } catch (BugFoundException e) {
            Bdd pc = e.pathConstraint;
            ReplayScheduler replay = new ReplayScheduler(scheduler.getSchedule(), pc);
            p.setScheduler(replay);
            replay.doSearch(scheduler.getStartMachine());
            e.printStackTrace();
            throw new BugFoundException("Found bug", pc);
        }
    }

    /*
    public static void main(String[] args) {
        Program twoPC = new timer();
        run(twoPC, 10);
    }
     */
}
