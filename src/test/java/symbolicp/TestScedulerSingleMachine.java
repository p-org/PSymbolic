package symbolicp;

import symbolicp.*;
        import symbolicp.bdd.*;
        import symbolicp.vs.*;
        import symbolicp.runtime.*;

import java.util.*;

public class TestScedulerSingleMachine {

    // Skipping Interface 'Main'

    static Scheduler scheduler;

    final static EventTag event_null = new EventTag("null", 0);
    final static EventTag event_halt = new EventTag("halt", 1);
    final static EventTag event_ACC = new EventTag("ACC", 2);

    final static MachineTag machineTag_Main = new MachineTag("Main", 0);

    // Skipping Interface 'Main'

    private static class machine_Main extends BaseMachine {
        private final static StateTag state_Init = new StateTag("Init", 0);
        private final static StateTag state_Send = new StateTag("Send", 1);
        private final static StateTag state_Stop = new StateTag("Stop", 2);

        private PrimVS<Integer> var_numIterations = new PrimVS<>(0);
        private PrimVS<Integer> var_pool = new PrimVS<>(0);

        machine_Main() {
            super(eventOps, machineTag_Main, 0, state_Init,
                    new State(state_Init
                    ) {
                        @Override public void entry(Bdd pc, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
                            ((machine_Main)machine).anonfunc_0(pc, machine.effectQueue, gotoOutcome);
                        }
                    },
                    new State(state_Send
                    ) {
                        @Override public void entry(Bdd pc, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
                            ((machine_Main)machine).anonfunc_1(pc, machine.effectQueue, gotoOutcome);
                        }
                    },
                    new State(state_Stop
                    ) {
                        @Override public void entry(Bdd pc, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
                            ((machine_Main)machine).anonfunc_2(pc, machine.effectQueue);
                        }
                    }
            );
        }

        Bdd
        anonfunc_0(
                Bdd pc_0,
                EffectQueue effects,
                GotoOutcome gotoOutcome
        ) {
            PrimVS<Integer> temp_var_0;
            temp_var_0 = ops_0.guard(new PrimVS<Integer>(10), pc_0);
            var_numIterations = ops_0.merge2(ops_0.guard(var_numIterations, pc_0.not()),temp_var_0);

            PrimVS<Integer> temp_var_1;
            temp_var_1 = ops_0.guard(new PrimVS<Integer>(0), pc_0);
            var_pool = ops_0.merge2(ops_0.guard(var_pool, pc_0.not()),temp_var_1);

            gotoOutcome.addGuardedGoto(pc_0, state_Send);
            pc_0 = Bdd.constFalse();

            return pc_0;
        }

        Bdd
        anonfunc_1(
                Bdd pc_1,
                EffectQueue effects,
                GotoOutcome gotoOutcome
        ) {
            PrimVS<Integer> var_$tmp0 =
                    ops_0.guard(new PrimVS<Integer>(0), pc_1);

            PrimVS<Boolean> var_$tmp1 =
                    ops_1.guard(new PrimVS<Boolean>(false), pc_1);

            PrimVS<Boolean> var_$tmp2 =
                    ops_1.guard(new PrimVS<Boolean>(false), pc_1);

            PrimVS<Integer> var_$tmp3 =
                    ops_0.guard(new PrimVS<Integer>(0), pc_1);

            PrimVS<Integer> temp_var_2;
            temp_var_2 = (ops_0.guard(var_pool, pc_1)).map2(ops_0.guard(new PrimVS<Integer>(1), pc_1), (temp_var_3, temp_var_4) -> temp_var_3 + temp_var_4);
            var_$tmp0 = ops_0.merge2(ops_0.guard(var_$tmp0, pc_1.not()),temp_var_2);

            PrimVS<Integer> temp_var_5;
            temp_var_5 = ops_0.guard(var_$tmp0, pc_1);
            var_pool = ops_0.merge2(ops_0.guard(var_pool, pc_1.not()),temp_var_5);

            PrimVS<Boolean> temp_var_6;
            temp_var_6 = (ops_0.guard(var_numIterations, pc_1)).map2(ops_0.guard(new PrimVS<Integer>(0), pc_1), (temp_var_7, temp_var_8) -> temp_var_7 == temp_var_8);
            var_$tmp1 = ops_1.merge2(ops_1.guard(var_$tmp1, pc_1.not()),temp_var_6);

            PrimVS<Boolean> temp_var_9 = ops_1.guard(var_$tmp1, pc_1);
            Bdd pc_2 = BoolUtils.trueCond(temp_var_9);
            Bdd pc_3 = BoolUtils.falseCond(temp_var_9);
            boolean jumpedOut_0 = false;
            boolean jumpedOut_1 = false;
            if (!pc_2.isConstFalse()) {
                // 'then' branch
                gotoOutcome.addGuardedGoto(pc_2, state_Stop);
                pc_2 = Bdd.constFalse();
                jumpedOut_0 = true;

            }
            if (!pc_3.isConstFalse()) {
                // 'else' branch
                PrimVS<Boolean> temp_var_10;
                temp_var_10 = (ops_0.guard(var_numIterations, pc_3)).map2(ops_0.guard(new PrimVS<Integer>(0), pc_3), (temp_var_11, temp_var_12) -> temp_var_11 > temp_var_12);
                var_$tmp2 = ops_1.merge2(ops_1.guard(var_$tmp2, pc_3.not()),temp_var_10);

                PrimVS<Boolean> temp_var_13 = ops_1.guard(var_$tmp2, pc_3);
                Bdd pc_4 = BoolUtils.trueCond(temp_var_13);
                Bdd pc_5 = BoolUtils.falseCond(temp_var_13);
                boolean jumpedOut_2 = false;
                boolean jumpedOut_3 = false;
                if (!pc_4.isConstFalse()) {
                    // 'then' branch
                    PrimVS<Integer> temp_var_14;
                    temp_var_14 = (ops_0.guard(var_numIterations, pc_4)).map2(ops_0.guard(new PrimVS<Integer>(1), pc_4), (temp_var_15, temp_var_16) -> temp_var_15 - temp_var_16);
                    var_$tmp3 = ops_0.merge2(ops_0.guard(var_$tmp3, pc_4.not()),temp_var_14);

                    PrimVS<Integer> temp_var_17;
                    temp_var_17 = ops_0.guard(var_$tmp3, pc_4);
                    var_numIterations = ops_0.merge2(ops_0.guard(var_numIterations, pc_4.not()),temp_var_17);

                }
                if (!pc_5.isConstFalse()) {
                    // 'else' branch
                }
                if (jumpedOut_2 || jumpedOut_3) {
                    pc_3 = pc_4.or(pc_5);
                    jumpedOut_1 = true;
                }

            }
            if (jumpedOut_0 || jumpedOut_1) {
                pc_1 = pc_2.or(pc_3);
            }

            if (!pc_1.isConstFalse()) {
                gotoOutcome.addGuardedGoto(pc_1, state_Send);
                pc_1 = Bdd.constFalse();

            }
            return pc_1;
        }

        void
        anonfunc_2(
                Bdd pc_6,
                EffectQueue effects
        ) {
            RuntimeLogger.log("done");

        }

    }

    // Skipping Implementation 'DefaultImpl'


    public static void main(String[] args) {
        BaseMachine main = new machine_Main();
        Map<MachineTag, List<BaseMachine>> machines = new HashMap<>();
        machines.put(machineTag_Main, new ArrayList<>(Arrays.asList(main)));
        Scheduler scheduler = new Scheduler(eventOps);
        main.start(Bdd.constTrue());
        int max_depth = 10;
        for (int i = 0; i < max_depth; ++i)
            if (scheduler.step()) break;
    }
    // Skipping Implementation 'DefaultImpl'

    private static final PrimVS.Ops<Integer> ops_0 =
            new PrimVS.Ops<Integer>();

    private static final PrimVS.Ops<Boolean> ops_1 =
            new PrimVS.Ops<Boolean>();

    final static EventVS.Ops eventOps = new EventVS.Ops(event_null, null, event_halt, null, event_ACC, null);
}
