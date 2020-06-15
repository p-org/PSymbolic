package symbolicp.run;
import symbolicp.runtime.Scheduler;
import symbolicp.*;

public class EntryPoint {

    public static void run(Program p, int depth) {
        Scheduler scheduler = new Scheduler();
        p.setScheduler(scheduler);
        scheduler.startWith(p.getStart());
        for (int i = 0; i < depth; i++) {
            scheduler.step();
        }
    }

}
