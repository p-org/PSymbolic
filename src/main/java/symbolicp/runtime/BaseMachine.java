package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;
import symbolicp.vs.PrimVS;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseMachine {
    private final StateTag startState;
    private final Map<StateTag, State> states;
    private static final RuntimeLogger LOGGER = new RuntimeLogger();

    private static final PrimVS.Ops<StateTag> stateOps = new PrimVS.Ops<>();
    private EventVS.Ops eventOps;

    private final MachineTag machineTag;
    private final int machineId;
    private String name;

    private PrimVS<StateTag> state;
    public final EffectQueue effectQueue;

    public BaseMachine(EventVS.Ops eventOps, MachineTag machineTag, int machineId, StateTag startState, State... states) {
        this.eventOps = eventOps;

        this.machineTag = machineTag;
        this.machineId = machineId;
        name = String.format("Machine %s #%d", this.machineTag, this.machineId);

        this.startState = startState;
        this.effectQueue = new EffectQueue(eventOps);

        //this.state = stateOps.empty()
        this.state = new PrimVS<>(startState);

        this.states = new HashMap<>();
        for (State state : states) {
            this.states.put(state.stateTag, state);
        }
    }

    public void start(Bdd pc) {
        
        this.state = stateOps.merge2(
            stateOps.guard(this.state, pc.not()),
            stateOps.guard(new PrimVS<>(startState), pc));

        GotoOutcome initGotoOutcome = new GotoOutcome();
        RaiseOutcome initRaiseOutcome = new RaiseOutcome(eventOps);
        states.get(startState).entry(pc, this, initGotoOutcome, initRaiseOutcome);

        runOutcomesToCompletion(initGotoOutcome, initRaiseOutcome);
    }

    void runOutcomesToCompletion(GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
        while (!(gotoOutcome.isEmpty() && raiseOutcome.isEmpty())) {
            GotoOutcome nextGotoOutcome = new GotoOutcome();
            RaiseOutcome nextRaiseOutcome = new RaiseOutcome(eventOps);
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
            GotoOutcome gotoOutcome, // 'out' parameter
            RaiseOutcome raiseOutcome, // 'out' parameter
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
            GotoOutcome gotoOutcome, // 'out' parameter
            RaiseOutcome raiseOutcome, // 'out' parameter
            EventVS event
    ) {
        LOGGER.onProcessEvent(pc, this, event);
        PrimVS<StateTag> guardedState = stateOps.guard(this.state, pc);
        for (Map.Entry<StateTag, Bdd> entry : guardedState.guardedValues.entrySet()) {
            Bdd state_pc = entry.getValue();
            EventVS guardedEvent = eventOps.guard(event, state_pc);
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

    void processEventToCompletion(Bdd pc, EventVS event) {
        final GotoOutcome emptyGotoOutcome = new GotoOutcome();
        final RaiseOutcome eventRaiseOutcome = new RaiseOutcome(eventOps);
        eventRaiseOutcome.addGuardedRaise(pc, event);
        runOutcomesToCompletion(emptyGotoOutcome, eventRaiseOutcome);
    }
}
