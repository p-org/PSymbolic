package symbolicp.testCase;
import symbolicp.*;
import symbolicp.bdd.*;
import symbolicp.vs.*;
import symbolicp.runtime.*;

public class accumulatortest {
    
    public static Scheduler scheduler;
    
    final static EventTag event_null = new EventTag("null", 0);
    final static EventTag event_halt = new EventTag("halt", 1);
    final static EventTag event_ACC = new EventTag("ACC", 2);
    
    public final static MachineTag machineTag_Main = new MachineTag("Main", 0);
    public final static MachineTag machineTag_AccumulatorMachine = new MachineTag("AccumulatorMachine", 1);
    
    // Skipping Interface 'Main'

    // Skipping Interface 'AccumulatorMachine'

    public static class machine_Main extends BaseMachine {
        private final static StateTag state_Init = new StateTag("Init", 0);
        private final static StateTag state_Send = new StateTag("Send", 1);
        private final static StateTag state_Stop = new StateTag("Stop", 2);
        
        private MachineRefVS var_accumulator = MachineRefVS.nullMachineRef();
        private PrimVS<Integer> var_numIterations = new PrimVS<Integer>(0);
        
        public machine_Main(int id) {
            super(eventOps, machineTag_Main, id, state_Init,
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
            MachineRefVS var_$tmp0 =
                ops_0.guard(MachineRefVS.nullMachineRef(), pc_0);
            
            PrimVS<Integer> temp_var_0;
            temp_var_0 = ops_1.guard(new PrimVS<Integer>(10), pc_0);
            var_numIterations = ops_1.merge2(ops_1.guard(var_numIterations, pc_0.not()),temp_var_0);
            
            MachineRefVS temp_var_1;
            temp_var_1 = effects.create(pc_0, scheduler, machineTag_AccumulatorMachine, (i) -> new machine_AccumulatorMachine(i));
            var_$tmp0 = ops_0.merge2(ops_0.guard(var_$tmp0, pc_0.not()),temp_var_1);
            
            MachineRefVS temp_var_2;
            temp_var_2 = ops_0.guard(var_$tmp0, pc_0);
            var_accumulator = ops_0.merge2(ops_0.guard(var_accumulator, pc_0.not()),temp_var_2);
            
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
            PrimVS<Boolean> var_$tmp0 =
                ops_2.guard(new PrimVS<Boolean>(false), pc_1);
            
            PrimVS<Boolean> var_$tmp1 =
                ops_2.guard(new PrimVS<Boolean>(false), pc_1);
            
            PrimVS<Integer> var_$tmp2 =
                ops_1.guard(new PrimVS<Integer>(0), pc_1);
            
            MachineRefVS var_$tmp3 =
                ops_0.guard(MachineRefVS.nullMachineRef(), pc_1);
            
            PrimVS<EventTag> var_$tmp4 =
                ops_3.guard(new PrimVS<EventTag>(event_null), pc_1);
            
            PrimVS<Integer> var_$tmp5 =
                ops_1.guard(new PrimVS<Integer>(0), pc_1);
            
            PrimVS<Boolean> temp_var_3;
            temp_var_3 = (ops_1.guard(var_numIterations, pc_1)).map2(ops_1.guard(new PrimVS<Integer>(0), pc_1), (temp_var_4, temp_var_5) -> temp_var_4 == temp_var_5);
            var_$tmp0 = ops_2.merge2(ops_2.guard(var_$tmp0, pc_1.not()),temp_var_3);
            
            PrimVS<Boolean> temp_var_6 = ops_2.guard(var_$tmp0, pc_1);
            Bdd pc_2 = BoolUtils.trueCond(temp_var_6);
            Bdd pc_3 = BoolUtils.falseCond(temp_var_6);
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
                PrimVS<Boolean> temp_var_7;
                temp_var_7 = (ops_1.guard(var_numIterations, pc_3)).map2(ops_1.guard(new PrimVS<Integer>(0), pc_3), (temp_var_8, temp_var_9) -> temp_var_8 > temp_var_9);
                var_$tmp1 = ops_2.merge2(ops_2.guard(var_$tmp1, pc_3.not()),temp_var_7);
                
                PrimVS<Boolean> temp_var_10 = ops_2.guard(var_$tmp1, pc_3);
                Bdd pc_4 = BoolUtils.trueCond(temp_var_10);
                Bdd pc_5 = BoolUtils.falseCond(temp_var_10);
                boolean jumpedOut_2 = false;
                boolean jumpedOut_3 = false;
                if (!pc_4.isConstFalse()) {
                    // 'then' branch
                    PrimVS<Integer> temp_var_11;
                    temp_var_11 = (ops_1.guard(var_numIterations, pc_4)).map2(ops_1.guard(new PrimVS<Integer>(1), pc_4), (temp_var_12, temp_var_13) -> temp_var_12 - temp_var_13);
                    var_$tmp2 = ops_1.merge2(ops_1.guard(var_$tmp2, pc_4.not()),temp_var_11);
                    
                    PrimVS<Integer> temp_var_14;
                    temp_var_14 = ops_1.guard(var_$tmp2, pc_4);
                    var_numIterations = ops_1.merge2(ops_1.guard(var_numIterations, pc_4.not()),temp_var_14);
                    
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
                MachineRefVS temp_var_15;
                temp_var_15 = ops_0.guard(var_accumulator, pc_1);
                var_$tmp3 = ops_0.merge2(ops_0.guard(var_$tmp3, pc_1.not()),temp_var_15);
                
                PrimVS<EventTag> temp_var_16;
                temp_var_16 = ops_3.guard(new PrimVS<EventTag>(event_ACC), pc_1);
                var_$tmp4 = ops_3.merge2(ops_3.guard(var_$tmp4, pc_1.not()),temp_var_16);
                
                PrimVS<Integer> temp_var_17;
                temp_var_17 = ops_1.guard(new PrimVS<Integer>(0), pc_1);
                var_$tmp5 = ops_1.merge2(ops_1.guard(var_$tmp5, pc_1.not()),temp_var_17);
                
                effects.send(pc_1, ops_0.guard(var_$tmp3, pc_1), ops_3.guard(var_$tmp4, pc_1), ops_1.guard(var_$tmp5, pc_1));
                
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
    
    public static class machine_AccumulatorMachine extends BaseMachine {
        private final static StateTag state_Init = new StateTag("Init", 0);
        private final static StateTag state_Wait = new StateTag("Wait", 1);
        private final static StateTag state_Accumulate = new StateTag("Accumulate", 2);
        
        private PrimVS<Integer> var_pool = new PrimVS<Integer>(0);
        
        public machine_AccumulatorMachine(int id) {
            super(eventOps, machineTag_AccumulatorMachine, id, state_Init,
                new State(state_Init
                ) {
                    @Override public void entry(Bdd pc, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
                        ((machine_AccumulatorMachine)machine).anonfunc_3(pc, machine.effectQueue, gotoOutcome);
                    }
                },
                new State(state_Wait,
                    new GotoEventHandler(event_ACC, state_Accumulate) 
                ) {
                },
                new State(state_Accumulate
                ) {
                    @Override public void entry(Bdd pc, BaseMachine machine, GotoOutcome gotoOutcome, RaiseOutcome raiseOutcome) {
                        ((machine_AccumulatorMachine)machine).anonfunc_4(pc, machine.effectQueue, gotoOutcome);
                    }
                }
            );
        }
        
        Bdd 
        anonfunc_3(
            Bdd pc_7,
            EffectQueue effects,
            GotoOutcome gotoOutcome
        ) {
            PrimVS<Integer> temp_var_18;
            temp_var_18 = ops_1.guard(new PrimVS<Integer>(0), pc_7);
            var_pool = ops_1.merge2(ops_1.guard(var_pool, pc_7.not()),temp_var_18);
            
            gotoOutcome.addGuardedGoto(pc_7, state_Wait);
            pc_7 = Bdd.constFalse();
            
            return pc_7;
        }
        
        Bdd 
        anonfunc_4(
            Bdd pc_8,
            EffectQueue effects,
            GotoOutcome gotoOutcome
        ) {
            PrimVS<Integer> var_$tmp0 =
                ops_1.guard(new PrimVS<Integer>(0), pc_8);
            
            RuntimeLogger.log("accumulated {0} ", ops_1.guard(var_pool, pc_8));
            
            PrimVS<Integer> temp_var_19;
            temp_var_19 = (ops_1.guard(var_pool, pc_8)).map2(ops_1.guard(new PrimVS<Integer>(1), pc_8), (temp_var_20, temp_var_21) -> temp_var_20 + temp_var_21);
            var_$tmp0 = ops_1.merge2(ops_1.guard(var_$tmp0, pc_8.not()),temp_var_19);
            
            PrimVS<Integer> temp_var_22;
            temp_var_22 = ops_1.guard(var_$tmp0, pc_8);
            var_pool = ops_1.merge2(ops_1.guard(var_pool, pc_8.not()),temp_var_22);
            
            gotoOutcome.addGuardedGoto(pc_8, state_Wait);
            pc_8 = Bdd.constFalse();
            
            return pc_8;
        }
        
    }
    
    // Skipping Implementation 'DefaultImpl'

    private static final MachineRefVS.Ops ops_0 =
        new MachineRefVS.Ops();
    
    private static final PrimVS.Ops<Integer> ops_1 =
        new PrimVS.Ops<Integer>();
    
    private static final PrimVS.Ops<Boolean> ops_2 =
        new PrimVS.Ops<Boolean>();
    
    private static final PrimVS.Ops<EventTag> ops_3 =
        new PrimVS.Ops<EventTag>();
    
    public final static EventVS.Ops eventOps = new EventVS.Ops(event_null, null, event_halt, null, event_ACC, ops_1);
    public static void main(String[] args) {
        // TODO: Make maxDepth configurable
        int maxDepth = 10;
        scheduler = new Scheduler(eventOps, machineTag_Main, machineTag_AccumulatorMachine);
        scheduler.startWith(machineTag_Main, new machine_Main(0));
        for (int i = 0; i < maxDepth; i++) {
            scheduler.step();
        }
    }
}
