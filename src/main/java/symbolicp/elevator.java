package symbolicp;

import symbolicp.*;
import symbolicp.bdd.*;
import symbolicp.vs.*;
import symbolicp.runtime.*;
import symbolicp.run.*;

public class elevator implements Program {

    public static Scheduler scheduler;

    @Override
    public void setScheduler (Scheduler s) { this.scheduler = s; }


    static enum Events implements EventName {
        event_null,
        event_halt,
        event_eOpenDoor,
        event_eCloseDoor,
        event_eResetDoor,
        event_eDoorOpened,
        event_eDoorClosed,
        event_eDoorStopped,
        event_eObjectDetected,
        event_eTimerFired,
        event_eOperationSuccess,
        event_eOperationFailure,
        event_eSendCommandToOpenDoor,
        event_eSendCommandToCloseDoor,
        event_eSendCommandToStopDoor,
        event_eSendCommandToResetDoor,
        event_eUnit,
        event_eStartTimer,
        event_eObjectEncountered,
    }

    // Skipping Interface 'Elevator'

    // Skipping Interface 'Main'

    // Skipping Interface 'Door'

    // Skipping Interface 'Timer'

    public static class machine_Elevator extends Machine {

        static State state_Init = new State("state_Init", 0) {
            @Override public void entry(Bdd pc_0, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Elevator)machine).anonfunc_0(pc_0, machine.sendEffects, raiseOutcome);
            }
        };
        static State state_DoorClosed = new State("state_DoorClosed", 1) {
            @Override public void entry(Bdd pc_1, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Elevator)machine).anonfunc_1(pc_1, machine.sendEffects);
            }
        };
        static State state_DoorOpening = new State("state_DoorOpening", 2) {
            @Override public void entry(Bdd pc_2, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Elevator)machine).anonfunc_2(pc_2, machine.sendEffects);
            }
        };
        static State state_DoorOpened = new State("state_DoorOpened", 3) {
            @Override public void entry(Bdd pc_3, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Elevator)machine).anonfunc_3(pc_3, machine.sendEffects);
            }
        };
        static State state_DoorOpenedOkToClose = new State("state_DoorOpenedOkToClose", 4) {
            @Override public void entry(Bdd pc_4, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Elevator)machine).anonfunc_4(pc_4, machine.sendEffects, raiseOutcome);
            }
        };
        static State state_DoorClosing = new State("state_DoorClosing", 5) {
            @Override public void entry(Bdd pc_5, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Elevator)machine).anonfunc_5(pc_5, machine.sendEffects);
            }
        };
        static State state_StoppingDoor = new State("state_StoppingDoor", 6) {
            @Override public void entry(Bdd pc_6, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Elevator)machine).anonfunc_6(pc_6, machine.sendEffects);
            }
        };
        private PrimVS<Machine> var_TimerV = new PrimVS<Machine>();
        private PrimVS<Machine> var_DoorV = new PrimVS<Machine>();

        public void reset() {
            super.reset();
            var_TimerV = new PrimVS<Machine>();
            var_DoorV = new PrimVS<Machine>();
        }
        public machine_Elevator(int id) {
            super("machine_Elevator", id, BufferSemantics.queue, state_Init, state_Init
                    , state_DoorClosed
                    , state_DoorOpening
                    , state_DoorOpened
                    , state_DoorOpenedOkToClose
                    , state_DoorClosing
                    , state_StoppingDoor

            );
            state_Init.addHandlers(new GotoEventHandler(Events.event_eUnit, state_DoorClosed));
            state_DoorClosed.addHandlers(new IgnoreEventHandler(Events.event_eCloseDoor),
                    new GotoEventHandler(Events.event_eOpenDoor, state_DoorOpening));
            state_DoorOpening.addHandlers(new IgnoreEventHandler(Events.event_eOpenDoor),
                    new DeferEventHandler(Events.event_eCloseDoor)
                    ,
                    new GotoEventHandler(Events.event_eDoorOpened, state_DoorOpened));
            state_DoorOpened.addHandlers(new DeferEventHandler(Events.event_eCloseDoor)
                    ,
                    new IgnoreEventHandler(Events.event_eOpenDoor),
                    new GotoEventHandler(Events.event_eTimerFired, state_DoorOpenedOkToClose));
            state_DoorOpenedOkToClose.addHandlers(new DeferEventHandler(Events.event_eOpenDoor)
                    ,
                    new GotoEventHandler(Events.event_eCloseDoor, state_DoorClosing));
            state_DoorClosing.addHandlers(new DeferEventHandler(Events.event_eCloseDoor)
                    ,
                    new GotoEventHandler(Events.event_eOpenDoor, state_StoppingDoor),
                    new GotoEventHandler(Events.event_eDoorClosed, state_DoorClosed),
                    new GotoEventHandler(Events.event_eObjectDetected, state_DoorOpening));
            state_StoppingDoor.addHandlers(new DeferEventHandler(Events.event_eCloseDoor)
                    ,
                    new IgnoreEventHandler(Events.event_eOpenDoor),
                    new IgnoreEventHandler(Events.event_eObjectDetected),
                    new GotoEventHandler(Events.event_eDoorOpened, state_DoorOpened),
                    new GotoEventHandler(Events.event_eDoorClosed, state_DoorClosed),
                    new GotoEventHandler(Events.event_eDoorStopped, state_DoorOpening));
        }

        Bdd
        anonfunc_0(
                Bdd pc_7,
                EffectCollection effects,
                RaiseOutcome raiseOutcome
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_7);

            PrimVS<Machine> var_$tmp1 =
                    new PrimVS<Machine>().guard(pc_7);

            PrimVS<Machine> var_$tmp2 =
                    new PrimVS<Machine>().guard(pc_7);

            PrimVS<Machine> var_$tmp3 =
                    new PrimVS<Machine>().guard(pc_7);

            PrimVS<EventName> var_$tmp4 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_7);

            PrimVS<Machine> temp_var_0;
            temp_var_0 = new PrimVS<Machine>(this).guard(pc_7);
            var_$tmp0 = var_$tmp0.update(pc_7, temp_var_0);

            PrimVS<Machine> temp_var_1;
            temp_var_1 = effects.create(pc_7, scheduler, machine_Timer.class, var_$tmp0.guard(pc_7), (i) -> new machine_Timer(i));
            var_$tmp1 = var_$tmp1.update(pc_7, temp_var_1);

            PrimVS<Machine> temp_var_2;
            temp_var_2 = var_$tmp1.guard(pc_7);
            var_TimerV = var_TimerV.update(pc_7, temp_var_2);

            PrimVS<Machine> temp_var_3;
            temp_var_3 = new PrimVS<Machine>(this).guard(pc_7);
            var_$tmp2 = var_$tmp2.update(pc_7, temp_var_3);

            PrimVS<Machine> temp_var_4;
            temp_var_4 = effects.create(pc_7, scheduler, machine_Door.class, var_$tmp2.guard(pc_7), (i) -> new machine_Door(i));
            var_$tmp3 = var_$tmp3.update(pc_7, temp_var_4);

            PrimVS<Machine> temp_var_5;
            temp_var_5 = var_$tmp3.guard(pc_7);
            var_DoorV = var_DoorV.update(pc_7, temp_var_5);

            PrimVS<EventName> temp_var_6;
            temp_var_6 = new PrimVS<EventName>(Events.event_eUnit).guard(pc_7);
            var_$tmp4 = var_$tmp4.update(pc_7, temp_var_6);

            // NOTE (TODO): We currently perform no typechecking on the payload!
            raiseOutcome.addGuardedRaise(pc_7, var_$tmp4.guard(pc_7));
            pc_7 = Bdd.constFalse();

            return pc_7;
        }

        void
        anonfunc_1(
                Bdd pc_8,
                EffectCollection effects
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_8);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_8);

            PrimVS<Machine> temp_var_7;
            temp_var_7 = var_DoorV.guard(pc_8);
            var_$tmp0 = var_$tmp0.update(pc_8, temp_var_7);

            PrimVS<EventName> temp_var_8;
            temp_var_8 = new PrimVS<EventName>(Events.event_eSendCommandToResetDoor).guard(pc_8);
            var_$tmp1 = var_$tmp1.update(pc_8, temp_var_8);

            effects.send(pc_8, var_$tmp0.guard(pc_8), var_$tmp1.guard(pc_8), null);

        }

        void
        anonfunc_2(
                Bdd pc_9,
                EffectCollection effects
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_9);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_9);

            PrimVS<Machine> temp_var_9;
            temp_var_9 = var_DoorV.guard(pc_9);
            var_$tmp0 = var_$tmp0.update(pc_9, temp_var_9);

            PrimVS<EventName> temp_var_10;
            temp_var_10 = new PrimVS<EventName>(Events.event_eSendCommandToOpenDoor).guard(pc_9);
            var_$tmp1 = var_$tmp1.update(pc_9, temp_var_10);

            effects.send(pc_9, var_$tmp0.guard(pc_9), var_$tmp1.guard(pc_9), null);

        }

        void
        anonfunc_3(
                Bdd pc_10,
                EffectCollection effects
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_10);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_10);

            PrimVS<Machine> var_$tmp2 =
                    new PrimVS<Machine>().guard(pc_10);

            PrimVS<EventName> var_$tmp3 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_10);

            PrimVS<Machine> temp_var_11;
            temp_var_11 = var_DoorV.guard(pc_10);
            var_$tmp0 = var_$tmp0.update(pc_10, temp_var_11);

            PrimVS<EventName> temp_var_12;
            temp_var_12 = new PrimVS<EventName>(Events.event_eSendCommandToResetDoor).guard(pc_10);
            var_$tmp1 = var_$tmp1.update(pc_10, temp_var_12);

            effects.send(pc_10, var_$tmp0.guard(pc_10), var_$tmp1.guard(pc_10), null);

            PrimVS<Machine> temp_var_13;
            temp_var_13 = var_TimerV.guard(pc_10);
            var_$tmp2 = var_$tmp2.update(pc_10, temp_var_13);

            PrimVS<EventName> temp_var_14;
            temp_var_14 = new PrimVS<EventName>(Events.event_eStartTimer).guard(pc_10);
            var_$tmp3 = var_$tmp3.update(pc_10, temp_var_14);

            effects.send(pc_10, var_$tmp2.guard(pc_10), var_$tmp3.guard(pc_10), null);

        }

        Bdd
        anonfunc_4(
                Bdd pc_11,
                EffectCollection effects,
                RaiseOutcome raiseOutcome
        ) {
            PrimVS<Boolean> var_$tmp0 =
                    new PrimVS<Boolean>(false).guard(pc_11);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_11);

            PrimVS<Boolean> temp_var_15;
            temp_var_15 = scheduler.getNextBoolean(pc_11);
            var_$tmp0 = var_$tmp0.update(pc_11, temp_var_15);

            PrimVS<Boolean> temp_var_16 = var_$tmp0.guard(pc_11);
            Bdd pc_12 = BoolUtils.trueCond(temp_var_16);
            Bdd pc_13 = BoolUtils.falseCond(temp_var_16);
            boolean jumpedOut_0 = false;
            boolean jumpedOut_1 = false;
            if (!pc_12.isConstFalse()) {
                // 'then' branch
                PrimVS<EventName> temp_var_17;
                temp_var_17 = new PrimVS<EventName>(Events.event_eCloseDoor).guard(pc_12);
                var_$tmp1 = var_$tmp1.update(pc_12, temp_var_17);

                // NOTE (TODO): We currently perform no typechecking on the payload!
                raiseOutcome.addGuardedRaise(pc_12, var_$tmp1.guard(pc_12));
                pc_12 = Bdd.constFalse();
                jumpedOut_0 = true;

            }
            if (!pc_13.isConstFalse()) {
                // 'else' branch
            }
            if (jumpedOut_0 || jumpedOut_1) {
                pc_11 = pc_12.or(pc_13);
            }

            if (!pc_11.isConstFalse()) {
            }
            return pc_11;
        }

        void
        anonfunc_5(
                Bdd pc_14,
                EffectCollection effects
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_14);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_14);

            PrimVS<Machine> temp_var_18;
            temp_var_18 = var_DoorV.guard(pc_14);
            var_$tmp0 = var_$tmp0.update(pc_14, temp_var_18);

            PrimVS<EventName> temp_var_19;
            temp_var_19 = new PrimVS<EventName>(Events.event_eSendCommandToCloseDoor).guard(pc_14);
            var_$tmp1 = var_$tmp1.update(pc_14, temp_var_19);

            effects.send(pc_14, var_$tmp0.guard(pc_14), var_$tmp1.guard(pc_14), null);

        }

        void
        anonfunc_6(
                Bdd pc_15,
                EffectCollection effects
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_15);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_15);

            PrimVS<Machine> temp_var_20;
            temp_var_20 = var_DoorV.guard(pc_15);
            var_$tmp0 = var_$tmp0.update(pc_15, temp_var_20);

            PrimVS<EventName> temp_var_21;
            temp_var_21 = new PrimVS<EventName>(Events.event_eSendCommandToStopDoor).guard(pc_15);
            var_$tmp1 = var_$tmp1.update(pc_15, temp_var_21);

            effects.send(pc_15, var_$tmp0.guard(pc_15), var_$tmp1.guard(pc_15), null);

        }

    }

    public static class machine_Main extends Machine {

        static State state_Init = new State("state_Init", 0) {
            @Override public void entry(Bdd pc_16, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Main)machine).anonfunc_7(pc_16, machine.sendEffects, gotoOutcome);
            }
        };
        static State state_Loop = new State("state_Loop", 1) {
            @Override public void entry(Bdd pc_17, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Main)machine).anonfunc_8(pc_17, machine.sendEffects, gotoOutcome);
            }
        };
        static State state_Done = new State("state_Done", 2) {
        };
        private PrimVS<Machine> var_ElevatorV = new PrimVS<Machine>();
        private PrimVS<Integer> var_count = new PrimVS<Integer>(0);

        public void reset() {
            super.reset();
            var_ElevatorV = new PrimVS<Machine>();
            var_count = new PrimVS<Integer>(0);
        }
        public machine_Main(int id) {
            super("machine_Main", id, BufferSemantics.queue, state_Init, state_Init
                    , state_Loop
                    , state_Done

            );
            state_Init.addHandlers();
            state_Loop.addHandlers();
            state_Done.addHandlers();
        }

        Bdd
        anonfunc_7(
                Bdd pc_18,
                EffectCollection effects,
                GotoOutcome gotoOutcome
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_18);

            PrimVS<Machine> temp_var_22;
            temp_var_22 = effects.create(pc_18, scheduler, machine_Elevator.class, (i) -> new machine_Elevator(i));
            var_$tmp0 = var_$tmp0.update(pc_18, temp_var_22);

            PrimVS<Machine> temp_var_23;
            temp_var_23 = var_$tmp0.guard(pc_18);
            var_ElevatorV = var_ElevatorV.update(pc_18, temp_var_23);

            gotoOutcome.addGuardedGoto(pc_18, state_Loop);
            pc_18 = Bdd.constFalse();

            return pc_18;
        }

        Bdd
        anonfunc_8(
                Bdd pc_19,
                EffectCollection effects,
                GotoOutcome gotoOutcome
        ) {
            System.out.println("loop");
            PrimVS<Boolean> var_$tmp0 =
                    new PrimVS<Boolean>(false).guard(pc_19);

            PrimVS<Machine> var_$tmp1 =
                    new PrimVS<Machine>().guard(pc_19);

            PrimVS<EventName> var_$tmp2 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_19);

            PrimVS<Boolean> var_$tmp3 =
                    new PrimVS<Boolean>(false).guard(pc_19);

            PrimVS<Machine> var_$tmp4 =
                    new PrimVS<Machine>().guard(pc_19);

            PrimVS<EventName> var_$tmp5 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_19);

            PrimVS<Boolean> var_$tmp6 =
                    new PrimVS<Boolean>(false).guard(pc_19);

            PrimVS<Integer> var_$tmp7 =
                    new PrimVS<Integer>(0).guard(pc_19);

            PrimVS<Boolean> temp_var_24;
            temp_var_24 = scheduler.getNextBoolean(pc_19);
            var_$tmp0 = var_$tmp0.update(pc_19, temp_var_24);

            PrimVS<Boolean> temp_var_25 = var_$tmp0.guard(pc_19);
            Bdd pc_20 = BoolUtils.trueCond(temp_var_25);
            Bdd pc_21 = BoolUtils.falseCond(temp_var_25);
            boolean jumpedOut_2 = false;
            boolean jumpedOut_3 = false;
            if (!pc_20.isConstFalse()) {
                System.out.println("open door branch");
                // 'then' branch
                PrimVS<Machine> temp_var_26;
                temp_var_26 = var_ElevatorV.guard(pc_20);
                var_$tmp1 = var_$tmp1.update(pc_20, temp_var_26);

                PrimVS<EventName> temp_var_27;
                temp_var_27 = new PrimVS<EventName>(Events.event_eOpenDoor).guard(pc_20);
                var_$tmp2 = var_$tmp2.update(pc_20, temp_var_27);

                effects.send(pc_20, var_$tmp1.guard(pc_20), var_$tmp2.guard(pc_20), null);

            }
            if (!pc_21.isConstFalse()) {
                // 'else' branch
                System.out.println("close door branch");
                PrimVS<Boolean> temp_var_28;
                temp_var_28 = scheduler.getNextBoolean(pc_21);
                var_$tmp3 = var_$tmp3.update(pc_21, temp_var_28);

                PrimVS<Boolean> temp_var_29 = var_$tmp3.guard(pc_21);
                Bdd pc_22 = BoolUtils.trueCond(temp_var_29);
                Bdd pc_23 = BoolUtils.falseCond(temp_var_29);
                boolean jumpedOut_4 = false;
                boolean jumpedOut_5 = false;
                if (!pc_22.isConstFalse()) {
                    // 'then' branch
                    PrimVS<Machine> temp_var_30;
                    temp_var_30 = var_ElevatorV.guard(pc_22);
                    var_$tmp4 = var_$tmp4.update(pc_22, temp_var_30);

                    PrimVS<EventName> temp_var_31;
                    temp_var_31 = new PrimVS<EventName>(Events.event_eCloseDoor).guard(pc_22);
                    var_$tmp5 = var_$tmp5.update(pc_22, temp_var_31);

                    effects.send(pc_22, var_$tmp4.guard(pc_22), var_$tmp5.guard(pc_22), null);

                }
                if (!pc_23.isConstFalse()) {
                    // 'else' branch
                }
                if (jumpedOut_4 || jumpedOut_5) {
                    pc_21 = pc_22.or(pc_23);
                    jumpedOut_3 = true;
                }

            }
            if (jumpedOut_2 || jumpedOut_3) {
                pc_19 = pc_20.or(pc_21);
            }

            PrimVS<Boolean> temp_var_32;
            temp_var_32 = (var_count.guard(pc_19)).apply2(new PrimVS<Integer>(2).guard(pc_19), (temp_var_33, temp_var_34) -> temp_var_33.equals(temp_var_34));
            var_$tmp6 = var_$tmp6.update(pc_19, temp_var_32);

            PrimVS<Boolean> temp_var_35 = var_$tmp6.guard(pc_19);
            Bdd pc_24 = BoolUtils.trueCond(temp_var_35);
            Bdd pc_25 = BoolUtils.falseCond(temp_var_35);
            boolean jumpedOut_6 = false;
            boolean jumpedOut_7 = false;
            if (!pc_24.isConstFalse()) {
                // 'then' branch
                gotoOutcome.addGuardedGoto(pc_24, state_Done);
                pc_24 = Bdd.constFalse();
                jumpedOut_6 = true;

            }
            if (!pc_25.isConstFalse()) {
                // 'else' branch
            }
            if (jumpedOut_6 || jumpedOut_7) {
                pc_19 = pc_24.or(pc_25);
            }

            if (!pc_19.isConstFalse()) {
                PrimVS<Integer> temp_var_36;
                temp_var_36 = (var_count.guard(pc_19)).apply2(new PrimVS<Integer>(1).guard(pc_19), (temp_var_37, temp_var_38) -> temp_var_37 + temp_var_38);
                var_$tmp7 = var_$tmp7.update(pc_19, temp_var_36);

                PrimVS<Integer> temp_var_39;
                temp_var_39 = var_$tmp7.guard(pc_19);
                var_count = var_count.update(pc_19, temp_var_39);

                gotoOutcome.addGuardedGoto(pc_19, state_Loop);
                pc_19 = Bdd.constFalse();

            }
            return pc_19;
        }

    }

    public static class machine_Door extends Machine {

        static State state__Init = new State("state__Init", 0) {
            @Override public void entry(Bdd pc_26, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Door)machine).anonfunc_9(pc_26, machine.sendEffects, gotoOutcome, payload != null ? (PrimVS<Machine>)payload : new PrimVS<Machine>().guard(pc_26));
            }
        };
        static State state_WaitForCommands = new State("state_WaitForCommands", 1) {
        };
        static State state_OpenDoor = new State("state_OpenDoor", 2) {
            @Override public void entry(Bdd pc_27, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Door)machine).anonfunc_10(pc_27, machine.sendEffects, raiseOutcome);
            }
        };
        static State state_ConsiderClosingDoor = new State("state_ConsiderClosingDoor", 3) {
            @Override public void entry(Bdd pc_28, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Door)machine).anonfunc_11(pc_28, machine.sendEffects, raiseOutcome);
            }
        };
        static State state_ObjectEncountered = new State("state_ObjectEncountered", 4) {
            @Override public void entry(Bdd pc_29, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Door)machine).anonfunc_12(pc_29, machine.sendEffects, gotoOutcome);
            }
        };
        static State state_CloseDoor = new State("state_CloseDoor", 5) {
            @Override public void entry(Bdd pc_30, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Door)machine).anonfunc_13(pc_30, machine.sendEffects, gotoOutcome);
            }
        };
        static State state_StopDoor = new State("state_StopDoor", 6) {
            @Override public void entry(Bdd pc_31, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Door)machine).anonfunc_14(pc_31, machine.sendEffects, gotoOutcome);
            }
        };
        static State state_ResetDoor = new State("state_ResetDoor", 7) {
        };
        private PrimVS<Machine> var_ElevatorV = new PrimVS<Machine>();

        public void reset() {
            super.reset();
            var_ElevatorV = new PrimVS<Machine>();
        }

        public machine_Door(int id) {
            super("machine_Door", id, BufferSemantics.queue, state__Init, state__Init
                    , state_WaitForCommands
                    , state_OpenDoor
                    , state_ConsiderClosingDoor
                    , state_ObjectEncountered
                    , state_CloseDoor
                    , state_StopDoor
                    , state_ResetDoor

            );
            state__Init.addHandlers();
            state_WaitForCommands.addHandlers(new IgnoreEventHandler(Events.event_eSendCommandToStopDoor),
                    new IgnoreEventHandler(Events.event_eSendCommandToResetDoor),
                    new IgnoreEventHandler(Events.event_eResetDoor),
                    new GotoEventHandler(Events.event_eSendCommandToOpenDoor, state_OpenDoor),
                    new GotoEventHandler(Events.event_eSendCommandToCloseDoor, state_ConsiderClosingDoor));
            state_OpenDoor.addHandlers(new GotoEventHandler(Events.event_eUnit, state_ResetDoor));
            state_ConsiderClosingDoor.addHandlers(new GotoEventHandler(Events.event_eUnit, state_CloseDoor),
                    new GotoEventHandler(Events.event_eObjectEncountered, state_ObjectEncountered),
                    new GotoEventHandler(Events.event_eSendCommandToStopDoor, state_StopDoor));
            state_ObjectEncountered.addHandlers();
            state_CloseDoor.addHandlers();
            state_StopDoor.addHandlers();
            state_ResetDoor.addHandlers(new IgnoreEventHandler(Events.event_eSendCommandToOpenDoor),
                    new IgnoreEventHandler(Events.event_eSendCommandToCloseDoor),
                    new IgnoreEventHandler(Events.event_eSendCommandToStopDoor),
                    new GotoEventHandler(Events.event_eSendCommandToResetDoor, state_WaitForCommands));
        }

        Bdd
        anonfunc_9(
                Bdd pc_32,
                EffectCollection effects,
                GotoOutcome gotoOutcome,
                PrimVS<Machine> var_payload
        ) {
            PrimVS<Machine> temp_var_40;
            temp_var_40 = var_payload.guard(pc_32);
            var_ElevatorV = var_ElevatorV.update(pc_32, temp_var_40);

            gotoOutcome.addGuardedGoto(pc_32, state_WaitForCommands);
            pc_32 = Bdd.constFalse();

            return pc_32;
        }

        Bdd
        anonfunc_10(
                Bdd pc_33,
                EffectCollection effects,
                RaiseOutcome raiseOutcome
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_33);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_33);

            PrimVS<EventName> var_$tmp2 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_33);

            PrimVS<Machine> temp_var_41;
            temp_var_41 = var_ElevatorV.guard(pc_33);
            var_$tmp0 = var_$tmp0.update(pc_33, temp_var_41);

            PrimVS<EventName> temp_var_42;
            temp_var_42 = new PrimVS<EventName>(Events.event_eDoorOpened).guard(pc_33);
            var_$tmp1 = var_$tmp1.update(pc_33, temp_var_42);

            effects.send(pc_33, var_$tmp0.guard(pc_33), var_$tmp1.guard(pc_33), null);

            PrimVS<EventName> temp_var_43;
            temp_var_43 = new PrimVS<EventName>(Events.event_eUnit).guard(pc_33);
            var_$tmp2 = var_$tmp2.update(pc_33, temp_var_43);

            // NOTE (TODO): We currently perform no typechecking on the payload!
            raiseOutcome.addGuardedRaise(pc_33, var_$tmp2.guard(pc_33));
            pc_33 = Bdd.constFalse();

            return pc_33;
        }

        Bdd
        anonfunc_11(
                Bdd pc_34,
                EffectCollection effects,
                RaiseOutcome raiseOutcome
        ) {
            PrimVS<Boolean> var_$tmp0 =
                    new PrimVS<Boolean>(false).guard(pc_34);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_34);

            PrimVS<Boolean> var_$tmp2 =
                    new PrimVS<Boolean>(false).guard(pc_34);

            PrimVS<EventName> var_$tmp3 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_34);

            PrimVS<Boolean> temp_var_44;
            temp_var_44 = scheduler.getNextBoolean(pc_34);
            var_$tmp0 = var_$tmp0.update(pc_34, temp_var_44);

            PrimVS<Boolean> temp_var_45 = var_$tmp0.guard(pc_34);
            Bdd pc_35 = BoolUtils.trueCond(temp_var_45);
            Bdd pc_36 = BoolUtils.falseCond(temp_var_45);
            boolean jumpedOut_8 = false;
            boolean jumpedOut_9 = false;
            if (!pc_35.isConstFalse()) {
                // 'then' branch
                PrimVS<EventName> temp_var_46;
                temp_var_46 = new PrimVS<EventName>(Events.event_eUnit).guard(pc_35);
                var_$tmp1 = var_$tmp1.update(pc_35, temp_var_46);

                // NOTE (TODO): We currently perform no typechecking on the payload!
                raiseOutcome.addGuardedRaise(pc_35, var_$tmp1.guard(pc_35));
                pc_35 = Bdd.constFalse();
                jumpedOut_8 = true;

            }
            if (!pc_36.isConstFalse()) {
                // 'else' branch
                PrimVS<Boolean> temp_var_47;
                temp_var_47 = scheduler.getNextBoolean(pc_36);
                var_$tmp2 = var_$tmp2.update(pc_36, temp_var_47);

                PrimVS<Boolean> temp_var_48 = var_$tmp2.guard(pc_36);
                Bdd pc_37 = BoolUtils.trueCond(temp_var_48);
                Bdd pc_38 = BoolUtils.falseCond(temp_var_48);
                boolean jumpedOut_10 = false;
                boolean jumpedOut_11 = false;
                if (!pc_37.isConstFalse()) {
                    // 'then' branch
                    PrimVS<EventName> temp_var_49;
                    temp_var_49 = new PrimVS<EventName>(Events.event_eObjectEncountered).guard(pc_37);
                    var_$tmp3 = var_$tmp3.update(pc_37, temp_var_49);

                    // NOTE (TODO): We currently perform no typechecking on the payload!
                    raiseOutcome.addGuardedRaise(pc_37, var_$tmp3.guard(pc_37));
                    pc_37 = Bdd.constFalse();
                    jumpedOut_10 = true;

                }
                if (!pc_38.isConstFalse()) {
                    // 'else' branch
                }
                if (jumpedOut_10 || jumpedOut_11) {
                    pc_36 = pc_37.or(pc_38);
                    jumpedOut_9 = true;
                }

                if (!pc_36.isConstFalse()) {
                }
            }
            if (jumpedOut_8 || jumpedOut_9) {
                pc_34 = pc_35.or(pc_36);
            }

            if (!pc_34.isConstFalse()) {
            }
            return pc_34;
        }

        Bdd
        anonfunc_12(
                Bdd pc_39,
                EffectCollection effects,
                GotoOutcome gotoOutcome
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_39);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_39);

            PrimVS<Machine> temp_var_50;
            temp_var_50 = var_ElevatorV.guard(pc_39);
            var_$tmp0 = var_$tmp0.update(pc_39, temp_var_50);

            PrimVS<EventName> temp_var_51;
            temp_var_51 = new PrimVS<EventName>(Events.event_eObjectDetected).guard(pc_39);
            var_$tmp1 = var_$tmp1.update(pc_39, temp_var_51);

            effects.send(pc_39, var_$tmp0.guard(pc_39), var_$tmp1.guard(pc_39), null);

            gotoOutcome.addGuardedGoto(pc_39, state_WaitForCommands);
            pc_39 = Bdd.constFalse();

            return pc_39;
        }

        Bdd
        anonfunc_13(
                Bdd pc_40,
                EffectCollection effects,
                GotoOutcome gotoOutcome
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_40);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_40);

            PrimVS<Machine> temp_var_52;
            temp_var_52 = var_ElevatorV.guard(pc_40);
            var_$tmp0 = var_$tmp0.update(pc_40, temp_var_52);

            PrimVS<EventName> temp_var_53;
            temp_var_53 = new PrimVS<EventName>(Events.event_eDoorClosed).guard(pc_40);
            var_$tmp1 = var_$tmp1.update(pc_40, temp_var_53);

            effects.send(pc_40, var_$tmp0.guard(pc_40), var_$tmp1.guard(pc_40), null);

            gotoOutcome.addGuardedGoto(pc_40, state_ResetDoor);
            pc_40 = Bdd.constFalse();

            return pc_40;
        }

        Bdd
        anonfunc_14(
                Bdd pc_41,
                EffectCollection effects,
                GotoOutcome gotoOutcome
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_41);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_41);

            PrimVS<Machine> temp_var_54;
            temp_var_54 = var_ElevatorV.guard(pc_41);
            var_$tmp0 = var_$tmp0.update(pc_41, temp_var_54);

            PrimVS<EventName> temp_var_55;
            temp_var_55 = new PrimVS<EventName>(Events.event_eDoorStopped).guard(pc_41);
            var_$tmp1 = var_$tmp1.update(pc_41, temp_var_55);

            effects.send(pc_41, var_$tmp0.guard(pc_41), var_$tmp1.guard(pc_41), null);

            gotoOutcome.addGuardedGoto(pc_41, state_OpenDoor);
            pc_41 = Bdd.constFalse();

            return pc_41;
        }

    }

    public static class machine_Timer extends Machine {

        static State state_Init = new State("state_Init", 0) {
            @Override public void entry(Bdd pc_42, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome, ValueSummary payload) {
                ((machine_Timer)machine).anonfunc_15(pc_42, machine.sendEffects, payload != null ? (PrimVS<Machine>)payload : new PrimVS<Machine>().guard(pc_42));
            }
        };
        private PrimVS<Machine> var_creator = new PrimVS<Machine>();

        public void reset() {
            super.reset();
            var_creator = new PrimVS<Machine>();
        }
        public machine_Timer(int id) {
            super("machine_Timer", id, BufferSemantics.queue, state_Init, state_Init

            );
            state_Init.addHandlers(new EventHandler(Events.event_eStartTimer) {
                @Override public void handleEvent(Bdd pc, ValueSummary payload, Machine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
                    ((machine_Timer)machine).anonfunc_16(pc, machine.sendEffects);
                }
            });
        }

        void
        anonfunc_15(
                Bdd pc_43,
                EffectCollection effects,
                PrimVS<Machine> var_client
        ) {
            PrimVS<Machine> temp_var_56;
            temp_var_56 = var_client.guard(pc_43);
            var_creator = var_creator.update(pc_43, temp_var_56);

        }

        void
        anonfunc_16(
                Bdd pc_44,
                EffectCollection effects
        ) {
            PrimVS<Machine> var_$tmp0 =
                    new PrimVS<Machine>().guard(pc_44);

            PrimVS<EventName> var_$tmp1 =
                    new PrimVS<EventName>(Events.event_null).guard(pc_44);

            PrimVS<Machine> temp_var_57;
            temp_var_57 = var_creator.guard(pc_44);
            var_$tmp0 = var_$tmp0.update(pc_44, temp_var_57);

            PrimVS<EventName> temp_var_58;
            temp_var_58 = new PrimVS<EventName>(Events.event_eTimerFired).guard(pc_44);
            var_$tmp1 = var_$tmp1.update(pc_44, temp_var_58);

            effects.send(pc_44, var_$tmp0.guard(pc_44), var_$tmp1.guard(pc_44), null);

        }

    }

    // Skipping Implementation 'DefaultImpl'

    private static Machine start = new machine_Main(0);

    @Override
    public Machine getStart() { return start; }

}
