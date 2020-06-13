package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.Checks;
import symbolicp.util.NotImplementedException;
import symbolicp.vs.*;

import java.util.*;
import java.util.function.Function;

public abstract class Machine extends HasId {
    private final State startState;
    private final Set<State> states;
    private PrimVS<Boolean> started = new PrimVS<>(false);
    int update = 0;

    private PrimVS<State> state;
    public final EffectQueue effectQueue;
    public final DeferQueue deferredQueue;

    public PrimVS<Boolean> hasStarted() {
        return started;
    }

    public PrimVS<State> getState() {
        return state;
    }

    public Machine(String name, int id, State startState, State... states) {
        super(name, id);

        this.startState = startState;
        this.effectQueue = new EffectQueue();
        this.deferredQueue = new DeferQueue();

        this.state = new PrimVS<>(startState);

        startState.addHandlers(
                new EventHandler(EventName.Init.instance) {
                    @Override
                    public void handleEvent(Bdd pc, ValueSummary payload, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
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

        GotoOutcome initGotoOutcome = new GotoOutcome();
        RaiseOutcome initRaiseOutcome = new RaiseOutcome();
        startState.entry(pc, this, initGotoOutcome, initRaiseOutcome, payload);

        runOutcomesToCompletion(pc, initGotoOutcome, initRaiseOutcome);
    }

    void runOutcomesToCompletion(Bdd pc, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
        ScheduleLogger.machineState(this);
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
                    Scheduler.schedule.withinStep(state.guard(pc).guard(gotoOutcome.getGotoCond()));
                    processStateTransition(
                            gotoOutcome.getGotoCond(),
                            nextGotoOutcome,
                            nextRaiseOutcome,
                            gotoOutcome.getStateSummary(),
                            gotoOutcome.getPayloads()
                    );
                }
                if (!raiseOutcome.isEmpty()) {
                    Scheduler.schedule.runEvent(raiseOutcome.getEventSummary().guard(raiseOutcome.getRaiseCond()));
                    processEvent(raiseOutcome.getRaiseCond(), nextGotoOutcome, nextRaiseOutcome, raiseOutcome.getEventSummary());
                }
                Event eventVS = raiseOutcome.getEventSummary();
                this.state.check();

                gotoOutcome = nextGotoOutcome;
                raiseOutcome = nextRaiseOutcome;
            }

            // Process events from the deferred queue
            pc = performedTransition.and(deferredQueue.enabledCond());
            if (!pc.isConstFalse()) {
                RaiseOutcome deferredRaiseOutcome = new RaiseOutcome();
                Event deferredEvent = deferredQueue.dequeueEntry(pc);
                deferredRaiseOutcome.addGuardedRaiseEvent(deferredEvent);
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
        ScheduleLogger.onProcessStateTransition(pc, this, newState);
        Scheduler.schedule.withinStep(newState.guard(pc));
        this.state.check();

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
            state.entry(transitionCond, this, gotoOutcome, raiseOutcome, payload);
        }
    }

    void processEvent(
            Bdd pc,
            GotoOutcome gotoOutcome, // 'out' parameter
            RaiseOutcome raiseOutcome, // 'out' parameter
            Event event
    ) {
        assert(Checks.includedIn(pc));
        assert(event.getMachine().guard(pc).getValues().size() <= 1);
        ScheduleLogger.onProcessEvent(pc, this, event);
        PrimVS<State> guardedState = this.state.guard(pc);
        for (GuardedValue<State> entry : guardedState.getGuardedValues()) {
            Bdd state_pc = entry.guard;
            if (state_pc.and(pc).isConstFalse()) continue;
            entry.value.handleEvent(event.guard(state_pc), this, gotoOutcome, raiseOutcome);
        }
    }

    void processEventToCompletion(Bdd pc, Event event) {
        assert(Checks.includedIn(pc));
        final GotoOutcome emptyGotoOutcome = new GotoOutcome();
        final RaiseOutcome eventRaiseOutcome = new RaiseOutcome();
        eventRaiseOutcome.addGuardedRaiseEvent(event);
        runOutcomesToCompletion(pc, emptyGotoOutcome, eventRaiseOutcome);
    }

    @Override
    public String toString() {
        return "Machine " + name + "#" + id;
    }
}
