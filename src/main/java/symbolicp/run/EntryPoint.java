package symbolicp.run;
import symbolicp.bdd.Bdd;
import symbolicp.runtime.Scheduler;
import symbolicp.*;

public class EntryPoint {

    public static void run(Program p, int depth) {
        Scheduler scheduler = new Scheduler();
        p.setScheduler(scheduler);
        scheduler.startWith(p.getStart());
        int step = 0;
        while (!scheduler.isDone()) {
            scheduler.step();
            Assert.prop(step < depth, "Max depth reached", Bdd.constTrue());
            step++;
        }
    }

}
