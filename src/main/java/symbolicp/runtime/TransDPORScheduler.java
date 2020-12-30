package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.GuardedValue;
import symbolicp.vs.PrimVS;

import java.util.ArrayList;
import java.util.List;

public class TransDPORScheduler extends BoundedScheduler {

    @Override
    public Schedule getNewSchedule() {
        return new DporSchedule();
    }

    public TransDPORScheduler(int senderBound, int boolBound, int intBound) {
        super(senderBound, boolBound, intBound);
    }

    @Override
    public void step() {
        PrimVS<Machine> choices = getNextSender();
        PrimVS<Machine> enabled = schedule.getSenderChoice(schedule.size() - 1);
        ((DporSchedule) schedule).compare(enabled);
        super.step();
    }

}
