package symbolicp;

import java.util.Map;

public class BaseMachine<StateTag, EventTag> {

    private Map<StateTag, State<StateTag, EventTag>> states;

    private PrimVS<StateTag> state;
    private PrimVS.Ops<StateTag> stateOps = new PrimVS.Ops<>();
    private EventVS.Ops<EventTag> eventOps;

    void runOutcomesToCompletion(GotoOutcome<StateTag> gotoOutcome, RaiseOutcome<EventTag> raiseOutcome) {
        while (!(gotoOutcome.isEmpty() && raiseOutcome.isEmpty())) {
            GotoOutcome<StateTag> nextGotoOutcome = new GotoOutcome<>();
            RaiseOutcome<EventTag> nextRaiseOutcome = new RaiseOutcome<>();
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
}
