package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;
import symbolicp.vs.PrimVS;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseMachine<MachineTag, StateTag, EventTag> {
    private final StateTag startState;
    private final Map<StateTag, State<StateTag, EventTag>> states;

    private PrimVS.Ops<StateTag> stateOps = new PrimVS.Ops<>();
    private EventVS.Ops<EventTag> eventOps;

    private PrimVS<StateTag> state;
    public final EffectQueue<MachineTag, EventTag> effectQueue;

    public BaseMachine(EventVS.Ops<EventTag> eventOps, StateTag startState, State<StateTag, EventTag>... states) {
        this.eventOps = eventOps;
        this.startState = startState;
        this.effectQueue = new EffectQueue<>(eventOps);

        this.states = new HashMap<>();
        for (State<StateTag, EventTag> state : states) {
            this.states.put(state.stateTag, state);
        }
    }

    public void start(Bdd pc) {
        this.state = stateOps.merge2(
            stateOps.guard(this.state, pc.not()),
            stateOps.guard(new PrimVS<>(startState), pc));

        GotoOutcome<StateTag> initGotoOutcome = new GotoOutcome<>();
        RaiseOutcome<EventTag> initRaiseOutcome = new RaiseOutcome<>(eventOps);
        states.get(startState).entry(pc, this, initGotoOutcome, initRaiseOutcome);

        runOutcomesToCompletion(initGotoOutcome, initRaiseOutcome);
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
    }

    void processEvent(
            Bdd pc,
            GotoOutcome<StateTag> gotoOutcome, // 'out' parameter
            RaiseOutcome<EventTag> raiseOutcome, // 'out' parameter
            EventVS<EventTag> event
    ) {
        PrimVS<StateTag> guardedState = stateOps.guard(this.state, pc);
        for (Map.Entry<StateTag, Bdd> entry : guardedState.guardedValues.entrySet()) {
            Bdd state_pc = entry.getValue();
            EventVS<EventTag> guardedEvent = eventOps.guard(event, state_pc);
            states.get(entry.getKey()).handleEvent(guardedEvent, this, gotoOutcome, raiseOutcome);
        }
    }

    void processEventToCompletion(Bdd pc, EventVS<EventTag> event) {
        final GotoOutcome<StateTag> emptyGotoOutcome = new GotoOutcome<>();
        final RaiseOutcome<EventTag> eventRaiseOutcome = new RaiseOutcome<>(eventOps);
        eventRaiseOutcome.addGuardedRaise(pc, event);
        runOutcomesToCompletion(emptyGotoOutcome, eventRaiseOutcome);
    }
}
