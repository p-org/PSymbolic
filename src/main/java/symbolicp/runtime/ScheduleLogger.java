package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScheduleLogger {

    private final static Logger log = Logger.getLogger("Schedule");

    /* If turned on, logs the path constraints and goto/raise outcomes */
    private static boolean isVerbose = false;

    public static void onProcessEvent(Bdd pc, Machine machine, PrimVS<Event> EventVS)
    {
        String msg = String.format("Machine %s processing event: %s", machine.toString(), EventVS);
        if (isVerbose) msg = String.format("under path %s ", pc) + msg;
        log.fine(msg);
    }

    public static void onProcessStateTransition(Bdd pc, Machine machine, PrimVS<State> newState) {
        String msg = String.format("Machine %s transitioning to state: %s", machine.toString(), newState);
        if (isVerbose) msg = String.format("under path %s ", pc) + msg;
        log.fine(msg);
    }

    public static void onMachineStart(Bdd pc, Machine machine) {
        String msg = String.format("Machine %s starting", machine.toString());
        if (isVerbose) msg = String.format("under path %s ", pc) + msg;
        log.fine(msg);
    }

    public static void machineState(Machine machine) {
        String msg = String.format("Machine %s in state %s", machine, machine.getState());
        log.info(msg);
    }

    public static void summarizeOutcomes(Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
        if (!isVerbose) return;
        String msg = String.format("Machine %s outcomes: Goto: %s Raise %s", machine.toString(), gotoOutcome, raiseOutcome);
        log.fine(msg);
    }

    /*
    public static void log(Object ... message) {
        base.info("<PrintLog> " + String.join(", ", Arrays.toString(message)));
    }
     */
    public static void setVerbose(boolean verbose) {
        isVerbose = verbose;
    }

    public static void finished(int steps) {
        log.info(String.format("Execution finished in %d steps", steps));
    }

    public static void handle(State st, Event event) {
        log.fine("State " + st.name + ", handling event of type " + event.name);
    }

    public static void disableInfo() {
        log.setLevel(Level.OFF);
    }

    public static void enableInfo() {
        log.setLevel(Level.INFO);
    }

    public static void schedule(List<EffectQueue.Effect> symbolicEffect, List<Machine> machines) {
        String msg = "Schedule: \n";
        for (EffectQueue.Effect effect : symbolicEffect) {
            msg += "    " + effect + "\n";
        }
        log.info(msg);
    }
}
