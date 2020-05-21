import symbolicp.bdd.Bdd;
import symbolicp.vs.BoolUtils;
import symbolicp.vs.MapVS;
import symbolicp.vs.PrimVS;

public class testsym {
    enum EventTag { event_null, event_halt }
    //final static UnionVS<EventTag>.Ops<EventTag> eventOps = new UnionVS<EventTag>.Ops<EventTag>(EventTag.event_null, null, EventTag.event_halt, null);
    static PrimVS<Integer>
    func_testequality(
            Bdd pc_0
    ) {
        PrimVS<Integer> var_x =
                ops_0.guard(new PrimVS<Integer>(0), pc_0);

        PrimVS<Integer> var_y =
                ops_0.guard(new PrimVS<Integer>(0), pc_0);

        MapVS<Integer, PrimVS<Integer>> var_w =
                ops_1.guard(new MapVS<Integer, PrimVS<Integer>>(), pc_0);

        PrimVS<Boolean> var_$tmp0 =
                ops_2.guard(new PrimVS<Boolean>(false), pc_0);

        PrimVS<Integer> var_$tmp1 =
                ops_0.guard(new PrimVS<Integer>(0), pc_0);

        PrimVS<Integer> var_$tmp2 =
                ops_0.guard(new PrimVS<Integer>(0), pc_0);

        PrimVS<Integer> retval = ops_0.empty();
        PrimVS<Integer> temp_var_0;
        temp_var_0 = ops_0.guard(new PrimVS<Integer>(10), pc_0);
        var_x = ops_0.merge2(ops_0.guard(var_x, pc_0.not()),temp_var_0);

        PrimVS<Integer> temp_var_1;
        temp_var_1 = ops_0.guard(new PrimVS<Integer>(24), pc_0);
        var_y = ops_0.merge2(ops_0.guard(var_y, pc_0.not()),temp_var_1);

        PrimVS<Boolean> temp_var_2;
        temp_var_2 = ops_2.guard(BoolUtils.fromTrueGuard(Bdd.newVar()), pc_0);
        var_$tmp0 = ops_2.merge2(ops_2.guard(var_$tmp0, pc_0.not()),temp_var_2);

        PrimVS<Boolean> temp_var_3 = ops_2.guard(var_$tmp0, pc_0);
        Bdd pc_1 = BoolUtils.trueCond(temp_var_3);
        Bdd pc_2 = BoolUtils.falseCond(temp_var_3);
        boolean jumpedOut_0 = false;
        boolean jumpedOut_1 = false;
        if (!pc_1.isConstFalse()) {
            // 'then' branch
            PrimVS<Integer> temp_var_4;
            temp_var_4 = ops_0.guard(new PrimVS<Integer>(5), pc_1);
            var_x = ops_0.merge2(ops_0.guard(var_x, pc_1.not()),temp_var_4);

            PrimVS<Integer> temp_var_5;
            temp_var_5 = ops_0.guard(new PrimVS<Integer>(5), pc_1);
            var_y = ops_0.merge2(ops_0.guard(var_y, pc_1.not()),temp_var_5);

            PrimVS<Integer> temp_var_6;
            temp_var_6 = ops_0.guard(new PrimVS<Integer>(3), pc_1);
            var_$tmp1 = ops_0.merge2(ops_0.guard(var_$tmp1, pc_1.not()),temp_var_6);

            MapVS<Integer, PrimVS<Integer>> temp_var_7 = ops_1.guard(var_w, pc_1);
            temp_var_7 = ops_1.add(temp_var_7, ops_0.guard(new PrimVS<Integer>(2), pc_1), ops_0.guard(var_$tmp1, pc_1));
            var_w = ops_1.merge2(ops_1.guard(var_w, pc_1.not()),temp_var_7);

            PrimVS<Integer> temp_var_8;
            temp_var_8 = ops_0.guard(var_y, pc_1);
            var_$tmp2 = ops_0.merge2(ops_0.guard(var_$tmp2, pc_1.not()),temp_var_8);

            MapVS<Integer, PrimVS<Integer>> temp_var_9 = ops_1.guard(var_w, pc_1);
            temp_var_9 = ops_1.add(temp_var_9, ops_0.guard(var_x, pc_1), ops_0.guard(var_$tmp2, pc_1));
            var_w = ops_1.merge2(ops_1.guard(var_w, pc_1.not()),temp_var_9);

        }
        if (!pc_2.isConstFalse()) {
            // 'else' branch
        }
        if (jumpedOut_0 || jumpedOut_1) {
            pc_0 = pc_1.or(pc_2);
        }

        retval = ops_0.merge2(retval, ops_0.guard(new PrimVS<Integer>(0), pc_0));
        pc_0 = Bdd.constFalse();

        return retval;
    }

    // Skipping Implementation 'DefaultImpl'

    private static final PrimVS.Ops<Integer> ops_0 =
            new PrimVS.Ops<Integer>();

    private static final MapVS.Ops<Integer, PrimVS<Integer>> ops_1 =
            new MapVS.Ops<Integer, PrimVS<Integer>>(ops_0);

    private static final PrimVS.Ops<Boolean> ops_2 =
            new PrimVS.Ops<Boolean>();

}
