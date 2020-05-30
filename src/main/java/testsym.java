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
                new PrimVS<Integer>(0).guard(pc_0);

        PrimVS<Integer> var_y =
                new PrimVS<Integer>(0).guard(pc_0);

        MapVS<Integer, PrimVS<Integer>> var_w =
                new MapVS<Integer, PrimVS<Integer>>(pc_0);

        PrimVS<Boolean> var_$tmp0 =
                new PrimVS<Boolean>(false).guard(pc_0);

        PrimVS<Integer> var_$tmp1 =
                new PrimVS<Integer>(0).guard(pc_0);

        PrimVS<Integer> var_$tmp2 =
                new PrimVS<Integer>(0).guard(pc_0);

        PrimVS<Integer> retval = new PrimVS<>();
        PrimVS<Integer> temp_var_0;
        temp_var_0 = new PrimVS<Integer>(10).guard(pc_0);
        var_x = var_x.guard(pc_0.not()).merge(temp_var_0);

        PrimVS<Integer> temp_var_1;
        temp_var_1 = new PrimVS<Integer>(24).guard(pc_0);
        var_y = var_y.guard(pc_0.not()).merge(temp_var_1);

        PrimVS<Boolean> temp_var_2;
        temp_var_2 = BoolUtils.fromTrueGuard(Bdd.newVar()).guard(pc_0);
        var_$tmp0 = var_$tmp0.guard(pc_0.not()).merge(temp_var_2);

        PrimVS<Boolean> temp_var_3 = var_$tmp0.guard(pc_0);
        Bdd pc_1 = BoolUtils.trueCond(temp_var_3);
        Bdd pc_2 = BoolUtils.falseCond(temp_var_3);
        boolean jumpedOut_0 = false;
        boolean jumpedOut_1 = false;
        if (!pc_1.isConstFalse()) {
            // 'then' branch
            PrimVS<Integer> temp_var_4;
            temp_var_4 = new PrimVS<Integer>(5).guard(pc_1);
            var_x = var_x.guard(pc_1.not()).merge(temp_var_4);

            PrimVS<Integer> temp_var_5;
            temp_var_5 = new PrimVS<Integer>(5).guard(pc_1);
            var_y = var_y.guard(pc_1.not()).merge(temp_var_5);

            PrimVS<Integer> temp_var_6;
            temp_var_6 = new PrimVS<Integer>(3).guard(pc_1);
            var_$tmp1 = var_$tmp1.guard(pc_1.not()).merge(temp_var_6);

            MapVS<Integer, PrimVS<Integer>> temp_var_7 = var_w.guard(pc_1);
            temp_var_7 = temp_var_7.add(new PrimVS<Integer>(2).guard(pc_1), var_$tmp1.guard(pc_1));
            var_w = var_w.guard(pc_1.not()).merge(temp_var_7);

            PrimVS<Integer> temp_var_8;
            temp_var_8 = var_y.guard(pc_1);
            var_$tmp2 = var_$tmp2.guard(pc_1.not()).merge(temp_var_8);

            MapVS<Integer, PrimVS<Integer>> temp_var_9 = var_w.guard(pc_1);
            temp_var_9 = temp_var_9.add(var_x.guard(pc_1), var_$tmp2.guard(pc_1));
            var_w = var_w.guard(pc_1.not()).merge(temp_var_9);

        }
        if (!pc_2.isConstFalse()) {
            // 'else' branch
        }
        if (jumpedOut_0 || jumpedOut_1) {
            pc_0 = pc_1.or(pc_2);
        }

        retval = retval.merge(new PrimVS<Integer>(0).guard(pc_0));
        pc_0 = Bdd.constFalse();

        return retval;
    }

    // Skipping Implementation 'DefaultImpl'

}
