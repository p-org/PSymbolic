package symbolicp;



import symbolicp.*;
import symbolicp.bdd.*;
import symbolicp.vs.*;
import symbolicp.runtime.*;

public class Test {
    enum EventTag { event_null, event_halt, event_e1, event_e2 }
    final static EventVS.Ops<EventTag> eventOps = new EventVS.Ops<EventTag>(EventTag.event_null, null, EventTag.event_halt, null, EventTag.event_e1, ops_1, EventTag.event_e2, ops_2);
    // Skipping Interface 'Foo'

    private static class machine_Foo extends BaseMachine<machine_Foo.StateTag, EventTag> {
        public enum StateTag { state_Init, state_State2, state_State3 }
        private PrimVS<Integer> var_x;

        machine_Foo() {
            super(eventOps, StateTag.state_Init,
                new State<StateTag, EventTag>(StateTag.state_Init,
                    new IgnoreEventHandler<StateTag, EventTag>(EventTag.event_e1),
                    new GotoEventHandler<StateTag, EventTag>(EventTag.event_e2, StateTag.state_State2)
                ) {
                    @Override public void entry(Bdd pc, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome, RaiseOutcome<EventTag> raiseOutcome) {
                        ((machine_Foo)machine).anonfunc_0(pc);
                    }
                    @Override public void exit(Bdd pc, BaseMachine machine) {
                        ((machine_Foo)machine).anonfunc_1(pc);
                    }
                },
                new State<StateTag, EventTag>(StateTag.state_State2,
                    new GotoEventHandler<StateTag, EventTag>(EventTag.event_e1, StateTag.state_Init) {
                        @Override public void transitionAction(Bdd pc, BaseMachine machine, Object payload) {
                            ((machine_Foo)machine).anonfunc_2(pc, (ListVS<PrimVS<Integer>>)payload);
                        }
                    },
                    new GotoEventHandler<StateTag, EventTag>(EventTag.event_e2, StateTag.state_State3)
                ) {
                    @Override public void entry(Bdd pc, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome, RaiseOutcome<EventTag> raiseOutcome) {
                        ((machine_Foo)machine).anonfunc_3(pc);
                    }
                },
                new State<StateTag, EventTag>(StateTag.state_State3,
                    new EventHandler<StateTag, EventTag>(EventTag.event_e2) {
                        @Override public void handleEvent(Bdd pc, Object payload, BaseMachine machine, GotoOutcome<StateTag> gotoOutcome, RaiseOutcome<EventTag> raiseOutcome) {
                            ((machine_Foo)machine).anonfunc_4(pc, gotoOutcome, (TupleVS)payload);
                        }
                    }
                ) {
                }
            );
        }

        void
        anonfunc_0(
            Bdd pc_0
        ) {
            PrimVS<Integer> temp_var_0;
            temp_var_0 = ops_0.guard(new PrimVS<Integer>(1), pc_0);
            var_x = ops_0.merge2(ops_0.guard(var_x, pc_0.not()),temp_var_0);

        }

        void
        anonfunc_1(
            Bdd pc_1
        ) {
            PrimVS<Integer> temp_var_1;
            temp_var_1 = ops_0.guard(new PrimVS<Integer>(2), pc_1);
            var_x = ops_0.merge2(ops_0.guard(var_x, pc_1.not()),temp_var_1);

        }

        void
        anonfunc_3(
            Bdd pc_2
        ) {
            PrimVS<Integer> temp_var_2;
            temp_var_2 = ops_0.guard(new PrimVS<Integer>(3), pc_2);
            var_x = ops_0.merge2(ops_0.guard(var_x, pc_2.not()),temp_var_2);

        }

        void
        anonfunc_2(
            Bdd pc_3,
            ListVS<PrimVS<Integer>> var_payload
        ) {
        }

        Bdd
        anonfunc_4(
            Bdd pc_4,
            GotoOutcome<StateTag> gotoOutcome,
            TupleVS var_payload
        ) {
            PrimVS<Integer> var_$tmp0 =
                ops_0.guard(new PrimVS<Integer>(0), pc_4);

            PrimVS<Integer> temp_var_3;
            temp_var_3 = (PrimVS<Integer>)(ops_2.guard(var_payload, pc_4)).getField(0);
            var_$tmp0 = ops_0.merge2(ops_0.guard(var_$tmp0, pc_4.not()),temp_var_3);

            PrimVS<Integer> temp_var_4;
            temp_var_4 = ops_0.guard(var_$tmp0, pc_4);
            var_x = ops_0.merge2(ops_0.guard(var_x, pc_4.not()),temp_var_4);

            gotoOutcome.addGuardedGoto(pc_4, StateTag.state_Init);
            pc_4 = Bdd.constFalse();

            return pc_4;
        }

    }
    // Skipping Implementation 'DefaultImpl'

    private static final PrimVS.Ops<Integer> ops_0 =
        new PrimVS.Ops<Integer>();

    private static final ListVS.Ops<PrimVS<Integer>> ops_1 =
        new ListVS.Ops<PrimVS<Integer>>(ops_0);

    private static final TupleVS.Ops ops_2 =
        new TupleVS.Ops(ops_0, ops_0);

}
