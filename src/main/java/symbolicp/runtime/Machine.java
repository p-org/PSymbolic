package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.bdd.Checks;
import symbolicp.vs.*;

import java.util.*;

public abstract class Machine extends HasId {
    private final State startState;
    private final Set<State> states;
    private static final RuntimeLogger LOGGER = new RuntimeLogger();
    private PrimVS<Boolean> started = new PrimVS<>(false);

    private PrimVS<State> state;
    public final EffectQueue effectQueue;
    public final DeferQueue deferredQueue;

    public PrimVS<Boolean> hasStarted() {
        return started;
    }

    public Machine(String name, int id, State startState, State... states) {
        super(name, id);

        this.startState = startState;
        this.effectQueue = new EffectQueue();
        this.deferredQueue = new DeferQueue();

        this.state = new PrimVS<>(startState);

        this.states = new HashSet<>();
        for (State state : states) {
            this.states.add(state);
        }
    }

    public void start(Bdd pc, ValueSummary payload) {
        this.state = this.state.guard(pc.not()).merge(new PrimVS<>(startState).guard(pc));
        this.started.update(pc, new PrimVS<Boolean>(true));

        GotoOutcome initGotoOutcome = new GotoOutcome();
        RaiseOutcome initRaiseOutcome = new RaiseOutcome();
        startState.entry(pc, this, initGotoOutcome, initRaiseOutcome, payload);

        runOutcomesToCompletion(pc, initGotoOutcome, initRaiseOutcome);
    }

    void runOutcomesToCompletion(Bdd pc, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
        // Outer loop: process sequences of 'goto's, 'raise's, and events from the deferred queue.
        while (!(gotoOutcome.isEmpty() && raiseOutcome.isEmpty())) {
            // TODO: Determine if this can be safely optimized into a concrete boolean
            Bdd performedTransition = Bdd.constFalse();

            // Inner loop: process sequences of 'goto's and 'raise's.
            while (!(gotoOutcome.isEmpty() && raiseOutcome.isEmpty())) {
                GotoOutcome nextGotoOutcome = new GotoOutcome();
                RaiseOutcome nextRaiseOutcome = new RaiseOutcome();
                if (!gotoOutcome.isEmpty()) {
                    performedTransition = performedTransition.or(gotoOutcome.getGotoCond());
                    processStateTransition(
                            gotoOutcome.getGotoCond(),
                            nextGotoOutcome,
                            nextRaiseOutcome,
                            gotoOutcome.getStateSummary(),
                            gotoOutcome.getPayloads()
                    );
                }
                if (!raiseOutcome.isEmpty()) {
                    processEvent(raiseOutcome.getRaiseCond(), nextGotoOutcome, nextRaiseOutcome, raiseOutcome.getEventSummary());
                }
                gotoOutcome = nextGotoOutcome;
                raiseOutcome = nextRaiseOutcome;
            }

            // Process events from the deferred queue
            pc = performedTransition.and(deferredQueue.enabledCond());
            if (!pc.isConstFalse()) {
                RaiseOutcome deferredRaiseOutcome = new RaiseOutcome();
                List<DeferQueue.Entry> deferredEvents = deferredQueue.dequeueEntry(pc);
                for (DeferQueue.Entry event : deferredEvents) {
                    deferredRaiseOutcome.addGuardedRaiseEvent(event.getCond(), event.event);
                }
                raiseOutcome = deferredRaiseOutcome;
            }
        }
    }

    void processStateTransition(
            Bdd pc,
            GotoOutcome gotoOutcome, // 'out' parameter
            RaiseOutcome raiseOutcome, // 'out' parameter
            PrimVS<State> newState,
            Map<State, ValueSummary> payloads
    ) {
        LOGGER.onProcessStateTransition(pc, this, newState);
        if (this.state == null) {
            this.state = newState;
        } else {
            PrimVS<State> guardedState = this.state.guard(pc);
            for (GuardedValue<State> entry : guardedState.getGuardedValues()) {
                entry.value.exit(entry.guard, this);
            }

            this.state = newState.merge(this.state.guard(pc.not()));
        }

        for (GuardedValue<State> entry : newState.getGuardedValues()) {
            State state = entry.value;
            Bdd transitionCond = entry.guard;
            ValueSummary payload = payloads.get(state);
            state.entry(transitionCond, this, gotoOutcome, raiseOutcome, payload);
        }
        LOGGER.summarizeOutcomes(this, gotoOutcome, raiseOutcome);
    }

    void processEvent(
            Bdd pc,
            GotoOutcome gotoOutcome, // 'out' parameter
            RaiseOutcome raiseOutcome, // 'out' parameter
            PrimVS<Event> event
    ) {
        LOGGER.onProcessEvent(pc, this, event);
        PrimVS<State> guardedState = this.state.guard(pc);
        for (GuardedValue<State> entry : guardedState.getGuardedValues()) {
            Bdd state_pc = entry.guard;
            PrimVS<Event> guardedEvent = event.guard(state_pc);
            if (state_pc.and(pc).isConstFalse()) continue;
            entry.value.handleEvent(guardedEvent, this, gotoOutcome, raiseOutcome);
        }
        LOGGER.summarizeOutcomes(this, gotoOutcome, raiseOutcome);
    }

    /*
    public MachineRefVS getMachineRef() {
        return new MachineRefVS(new PrimVS<>(getMachineTag()), new PrimVS<>(getMachineId()));
    }
    */

    void processEventToCompletion(Bdd pc, PrimVS<Event> event) {
        final GotoOutcome emptyGotoOutcome = new GotoOutcome();
        final RaiseOutcome eventRaiseOutcome = new RaiseOutcome();
        eventRaiseOutcome.addGuardedRaiseEvent(pc, event);
        runOutcomesToCompletion(pc, emptyGotoOutcome, eventRaiseOutcome);
    }

    @Override
    public String toString() {
        return "Machine " + name + "#" + id;
    }
}
