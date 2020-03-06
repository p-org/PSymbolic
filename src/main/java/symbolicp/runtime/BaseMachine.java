package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;
import symbolicp.vs.PrimVS;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseMachine<StateTag, EventTag> {
    private final StateTag startState;
    private final Map<StateTag, State<StateTag, EventTag>> states;
    private static final RuntimeLogger LOGGER = new RuntimeLogger();

    private PrimVS.Ops<StateTag> stateOps = new PrimVS.Ops<>();
    private EventVS.Ops<EventTag> eventOps;

    private PrimVS<StateTag> state;
    private int machineId;
    private String name;
    private static int machineIdCounter;

    public BaseMachine(EventVS.Ops<EventTag> eventOps, StateTag startState, State<StateTag, EventTag>... states) {
        this.eventOps = eventOps;
        this.startState = startState;

        this.states = new HashMap<>();
        for (State<StateTag, EventTag> state : states) {
            this.states.put(state.stateTag, state);
        }
        machineId = machineIdCounter;
        machineIdCounter ++;
        name = String.format("Machine #%d", machineId);
    }

    public void start(Bdd pc) {
        LOGGER.onMachineStart(pc, this);
        GotoOutcome<StateTag> initGoto = new GotoOutcome<>();
        initGoto.addGuardedGoto(pc, startState);

        RaiseOutcome<EventTag> emptyRaise = new RaiseOutcome<>(eventOps);

        runOutcomesToCompletion(initGoto, emptyRaise);
        LOGGER.summarizeOutcomes(this, initGoto, emptyRaise);
    }

    void runOutcomesToCompletion(GotoOutcome<StateTag> gotoOutcome, RaiseOutcome<EventTag> raiseOutcome) {
        while (!(gotoOutcome.isEmpty() && raiseOutcome.isEmpty())) {
            GotoOutcome<StateTag> nextGotoOutcome = new GotoOutcome<>();
            RaiseOutcome<EventTag> nextRaiseOutcome = new RaiseOutcome<>(eventOps);
            if (!gotoOutcome.isEmpty()) {
                processStateTransition(gotoOutcome.getGotoCond(), nextGotoOutcome, nextRaiseOutcome, gotoOutcome.getStateSummary());
            }
            if (!raiseOutcome.isEmpty()) {
                processEvent(raiseOutcome.getRaiseCond(), nextGotoOutcome, nextRaiseOutcome, raiseOutcome.getEventSummary());
            }
            gotoOutcome = nextGotoOutcome;
            raiseOutcome = nextRaiseOutcome;
        }
    }

    void processStateTransition(
            Bdd pc,
            GotoOutcome<StateTag> gotoOutcome, // 'out' parameter
            RaiseOutcome<EventTag> raiseOutcome, // 'out' parameter
            PrimVS<StateTag> newState
    ) {
        LOGGER.onProcessStateTransition(pc, this, newState);
        if (this.state == null) {
            this.state = newState;
        } else {
            PrimVS<StateTag> guardedState = stateOps.guard(this.state, pc);
            for (Map.Entry<StateTag, Bdd> entry : guardedState.guardedValues.entrySet()) {
                states.get(entry.getKey()).exit(entry.getValue(), this);
            }

            this.state = stateOps.merge2(newState, stateOps.guard(this.state, pc.not()));
        }

        for (Map.Entry<StateTag, Bdd> entry : newState.guardedValues.entrySet()) {
            states.get(entry.getKey()).entry(entry.getValue(), this, gotoOutcome, raiseOutcome);
        }
        LOGGER.summarizeOutcomes(this, gotoOutcome, raiseOutcome);
    }

    void processEvent(
            Bdd pc,
            GotoOutcome<StateTag> gotoOutcome, // 'out' parameter
            RaiseOutcome<EventTag> raiseOutcome, // 'out' parameter
            EventVS<EventTag> event
    ) {
        LOGGER.onProcessEvent(pc, this, event);
        PrimVS<StateTag> guardedState = stateOps.guard(this.state, pc);
        for (Map.Entry<StateTag, Bdd> entry : guardedState.guardedValues.entrySet()) {
            Bdd state_pc = entry.getValue();
            EventVS<EventTag> guardedEvent = eventOps.guard(event, state_pc);
            states.get(entry.getKey()).handleEvent(guardedEvent, this, gotoOutcome, raiseOutcome);
        }
        LOGGER.summarizeOutcomes(this, gotoOutcome, raiseOutcome);
    }

    int getMachineId() {
        return machineId;
    }

    void setName(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }
}
