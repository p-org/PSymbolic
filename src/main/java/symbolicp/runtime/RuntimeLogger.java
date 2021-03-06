package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RuntimeLogger {

    private final static Logger base = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /* If turned on, logs the path constraints and goto/raise outcomes */
    private boolean isVerbose = false;

    public void onProcessEvent(Bdd pc, BaseMachine machine, UnionVS<EventTag> EventVS)
    {
        base.entering("BaseMachine", "processEvent");
        String msg = String.format("machine %s processing event: %s", machine.getName(), EventVS);
        if (isVerbose) msg = String.format("under path %s ", pc) + msg;
        base.info(msg);
    }

    public void onProcessStateTransition(Bdd pc, BaseMachine machine, PrimVS newState) {
        base.entering("BaseMachine", "processStateTransition");
        String msg = String.format("machine %s transitioning to state: %s", machine.getName(), newState);
        if (isVerbose) msg = String.format("under path %s ", pc) + msg;
        base.info(msg);
    }

    public void onMachineStart(Bdd pc, BaseMachine machine) {
        base.entering("BaseMachine", "start");
        String msg = String.format("machine %s starting", machine.getMachineId());
        if (isVerbose) msg = String.format("under path %s ", pc) + msg;
        base.info(msg);
    }

    public void summarizeOutcomes(BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
        if (!isVerbose) return;
        String msg = String.format("machine %s outcomes: Goto: %s Raise %s", machine.getName(), gotoOutcome, raiseOutcome);
        base.info(msg);
    }

    public static void log(Object ... message) {
        base.info("<PrintLog> " + String.join(", ", Arrays.toString(message)));
    }

    public void setVerbose(boolean verbose) {
        isVerbose = verbose;
    }

    public static void disableInfo() {
        base.setLevel(Level.OFF);
    }

    public static void enableInfo() {
        base.setLevel(Level.INFO);
    }


}
