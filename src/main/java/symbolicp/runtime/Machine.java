package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.*;
import java.util.logging.Logger;

public abstract class Machine extends HasId {
    private final State startState;
    private final Set<State> states;
    private PrimVS<Boolean> started = new PrimVS<>(false);
    int update = 0;

    private PrimVS<State> state;
    public final EffectCollection sendEffects;
    public final DeferQueue deferredQueue;

    public PrimVS<Boolean> hasStarted() {
        return started;
    }

    public PrimVS<State> getState() {
        return state;
    }

    public void reset() {
        started = new PrimVS<>(false);
        state = new PrimVS<>(startState);
        while (!sendEffects.isEmpty()) {
            Bdd cond = sendEffects.enabledCond(x -> new PrimVS<>(true)).getGuard(true);
            sendEffects.remove(sendEffects.enabledCond(x -> new PrimVS<>(true)).getGuard(true));
        }
        while (!deferredQueue.isEmpty()) {
            deferredQueue.dequeueEntry(deferredQueue.enabledCond(x -> new PrimVS<>(true)).getGuard(true));
        }
    }

    public Machine(String name, int id, BufferSemantics semantics, State startState, State... states) {
        super(name, id);

        EffectCollection buffer;
        switch (semantics) {
            case bag:
                buffer = new EffectBag();
                break;
            default:
                buffer = new EffectQueue();
                break;
        }

        this.startState = startState;
        this.sendEffects = buffer;
        this.deferredQueue = new DeferQueue();

        this.state = new PrimVS<>(startState);

        startState.addHandlers(
                new EventHandler(EventName.Init.instance) {
                    @Override
                    public void handleEvent(Bdd pc, ValueSummary payload, Machine machine, Outcome outcome) {
                        assert(!BoolUtils.isEverTrue(hasStarted().guard(pc)));
                        machine.start(pc, payload);
                    }
                }
        );

        this.states = new HashSet<>();
        for (State state : states) {
            this.states.add(state);
        }
    }

    public void start(Bdd pc, ValueSummary payload) {
        ScheduleLogger.onMachineStart(pc, this);
        update++;
        this.state = this.state.guard(pc.not()).merge(new PrimVS<>(startState).guard(pc));
        this.started = this.started.update(pc, new PrimVS<>(true));

        Outcome initOutcome = new Outcome();
        startState.entry(pc, this, initOutcome, payload);

        runOutcomesToCompletion(pc, initOutcome);
    }

    void runOutcomesToCompletion(Bdd pc, Outcome outcome) {
        // Outer loop: process sequences of 'goto's, 'raise's, and events from the deferred queue.
        while (!outcome.isEmpty()) {
            // TODO: Determine if this can be safely optimized into a concrete boolean
            Bdd performedTransition = Bdd.constFalse();
            Logger.getLogger("run again");

            // Inner loop: process sequences of 'goto's and 'raise's.
            while (!outcome.isEmpty()) {
                Outcome nextOutcome = new Outcome();
                if (!outcome.getGotoCond().isConstFalse()) {
                    performedTransition = performedTransition.or(outcome.getGotoCond());
                    processStateTransition(
                            outcome.getGotoCond(),
                            nextOutcome,
                            outcome.getStateSummary(),
                            outcome.getPayloads()
                    );
                }
                if (!outcome.getRaiseCond().isConstFalse()) {
                    processEvent(outcome.getRaiseCond(), nextOutcome, outcome.getEventSummary());
                }
                Event eventVS = outcome.getEventSummary();
                this.state.check();

                outcome = nextOutcome;
            }

            // Process events from the deferred queue
            pc = performedTransition.and(deferredQueue.enabledCond());
            if (!pc.isConstFalse()) {
                Outcome deferredRaiseOutcome = new Outcome();
                Event deferredEvent = deferredQueue.dequeueEntry(pc);
                deferredRaiseOutcome.addGuardedRaiseEvent(deferredEvent);
                outcome = deferredRaiseOutcome;
            }
        }
    }

    void processStateTransition(
            Bdd pc,
            Outcome outcome, // 'out' parameter
            PrimVS<State> newState,
            Map<State, ValueSummary> payloads
    ) {
        ScheduleLogger.onProcessStateTransition(pc, this, newState);

        if (this.state == null) {
            this.state = newState;
        } else {
            PrimVS<State> guardedState = this.state.guard(pc);
            for (GuardedValue<State> entry : guardedState.getGuardedValues()) {
                entry.value.exit(entry.guard, this);
            }

            this.state = newState.merge(this.state.guard(pc.not()));
        }

        this.state.check();

        for (GuardedValue<State> entry : newState.guard(pc).getGuardedValues()) {
            State state = entry.value;
            Bdd transitionCond = entry.guard;
            ValueSummary payload = payloads.get(state);
            state.entry(transitionCond, this, outcome, payload);
        }
    }

    void processEvent(
            Bdd pc,
            Outcome outcome,
            Event event
    ) {
        assert(event.getMachine().guard(pc).getValues().size() <= 1);
        ScheduleLogger.onProcessEvent(pc, this, event);
        PrimVS<State> guardedState = this.state.guard(pc);
        for (GuardedValue<State> entry : guardedState.getGuardedValues()) {
            Bdd state_pc = entry.guard;
            if (state_pc.and(pc).isConstFalse()) continue;
            entry.value.handleEvent(event.guard(state_pc), this, outcome);
        }
    }

    void processEventToCompletion(Bdd pc, Event event) {
        final Outcome eventRaiseOutcome = new Outcome();
        eventRaiseOutcome.addGuardedRaiseEvent(event);
        runOutcomesToCompletion(pc, eventRaiseOutcome);
    }

    @Override
    public String toString() {
        return name + "#" + id;
    }
}
