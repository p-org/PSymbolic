package symbolicp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import symbolicp.bdd.Bdd;
import symbolicp.vs.ListVS;
import symbolicp.vs.MapVS;
import symbolicp.vs.PrimVS;
import symbolicp.vs.SetVS;



public class TestValueSummaries {

    private static final PrimVS.Ops<Integer> ops_int = new PrimVS.Ops<>();
    private static final SetVS.Ops<Integer> ops_set = new SetVS.Ops<>();
    private static final ListVS.Ops<PrimVS<Integer>> ops_list = new ListVS.Ops<>(ops_int);
    private static final MapVS.Ops<Integer, PrimVS<Integer>> ops_map = new MapVS.Ops<>(ops_int);

    @Test
    public void testPrimVS()
    {
        Bdd path = Bdd.constTrue();
        Bdd bdd1 = Bdd.newVar();

        PrimVS<Integer> var_x = ops_int.guard(new PrimVS<Integer>(0), path);
        PrimVS<Integer> var_y = ops_int.guard(new PrimVS<Integer>(0), path);
        PrimVS<Integer> var_xg = ops_int.guard(new PrimVS<Integer>(1), bdd1);
        PrimVS<Integer> var_yg = ops_int.guard(new PrimVS<Integer>(1), bdd1.not());

        assertTrue(ops_int.symbolicEquals(var_x, var_y, path).guardedValues
                .getOrDefault(true, Bdd.constFalse()).isConstTrue());

        PrimVS<Integer> var_x1 = ops_int.merge2(ops_int.guard(var_x, bdd1.not()), var_xg);
        PrimVS<Integer> var_y1 = ops_int.merge2(ops_int.guard(var_y, bdd1), var_yg);


        assertTrue(ops_int.symbolicEquals(var_x1, var_y1, path).guardedValues
                .getOrDefault(false, Bdd.constFalse()).isConstTrue());
    }

    @Test
    public void testSetVS()
    {
        Bdd path = Bdd.constTrue();

        PrimVS<Integer> var_x0 = ops_int.guard(new PrimVS<Integer>(0), path);
        PrimVS<Integer> var_y0 = ops_int.guard(new PrimVS<Integer>(0), path);
        PrimVS<Integer> var_x1 = ops_int.guard(new PrimVS<Integer>(1), path);
        PrimVS<Integer> var_y1 = ops_int.guard(new PrimVS<Integer>(1), path);

        SetVS<Integer> set_0 = ops_set.guard(new SetVS<>(), path);
        set_0 = ops_set.add(set_0, var_x0);
        set_0 = ops_set.add(set_0, var_x1);

        SetVS<Integer> set_1 = ops_set.guard(new SetVS<>(), path);
        set_1 = ops_set.add(set_1, var_y0);
        set_1 = ops_set.add(set_1, var_y1);

        assertTrue(ops_set.symbolicEquals(set_0, set_1, path).guardedValues
                .getOrDefault(true, Bdd.constFalse()).isConstTrue());

        set_1 = ops_set.remove(set_1, var_y0);

        assertTrue(ops_set.symbolicEquals(set_1, set_0, path).guardedValues
                .getOrDefault(false, Bdd.constFalse()).isConstTrue());
    }

    @Test
    public void testMapVS()
    {
        Bdd path = Bdd.constTrue();

        PrimVS<Integer> var_x0 = ops_int.guard(new PrimVS<Integer>(0), path);
        PrimVS<Integer> var_y0 = ops_int.guard(new PrimVS<Integer>(0), path);
        PrimVS<Integer> var_x1 = ops_int.guard(new PrimVS<Integer>(1), path);
        PrimVS<Integer> var_y1 = ops_int.guard(new PrimVS<Integer>(1), path);
        PrimVS<Integer> var_y2 = ops_int.guard(new PrimVS<Integer>(2), path);

        MapVS<Integer, PrimVS<Integer>> map_0 = new MapVS<>();
        MapVS<Integer, PrimVS<Integer>> map_1 = new MapVS<>();

        assertTrue(ops_map.symbolicEquals(map_0, map_1, path).guardedValues
                .getOrDefault(true, Bdd.constFalse()).isConstTrue());

        map_0 = ops_map.add(map_0, var_x0, var_x1);
        map_1 = ops_map.add(map_1, var_y0, var_y1);

        assertTrue(ops_map.symbolicEquals(map_0, map_1, path).guardedValues
                .getOrDefault(true, Bdd.constFalse()).isConstTrue());

        //map_0 = ops_map.add(map_0, var_y2, var_x0);
        map_1 = ops_map.add(map_1, var_y2, var_y1);
        assertTrue(ops_map.symbolicEquals(map_0, map_1, path).guardedValues
                .getOrDefault(false, Bdd.constFalse()).isConstTrue());
    }

    @Test
    public void testListVS()
    {
        Bdd path = Bdd.constTrue();

        PrimVS<Integer> var_x0 = ops_int.guard(new PrimVS<Integer>(0), path);
        PrimVS<Integer> var_y0 = ops_int.guard(new PrimVS<Integer>(0), path);
        PrimVS<Integer> var_x1 = ops_int.guard(new PrimVS<Integer>(1), path);
        PrimVS<Integer> var_y1 = ops_int.guard(new PrimVS<Integer>(1), path);
        PrimVS<Integer> var_y2 = ops_int.guard(new PrimVS<Integer>(2), path);
        PrimVS<Integer> var_x2 = ops_int.guard(new PrimVS<Integer>(2), path);
        PrimVS<Integer> var_y3 = ops_int.guard(new PrimVS<Integer>(2), path);

        ListVS<PrimVS<Integer>> list_0 = new ListVS<>();
        ListVS<PrimVS<Integer>> list_1 = new ListVS<>();

        assertTrue(ops_list.symbolicEquals(list_0, list_1, path).guardedValues
                .getOrDefault(true, Bdd.constFalse()).isConstTrue());

        list_0 = ops_list.add(list_0, var_x0);
        list_0 = ops_list.add(list_0, var_x1);
        list_0 = ops_list.add(list_0, var_x2);

        list_1 = ops_list.add(list_1, var_y3);
        list_1 = ops_list.add(list_1, var_y0);
        list_1 = ops_list.add(list_1, var_y1);
        list_1 = ops_list.add(list_1, var_y2);

        assertTrue(ops_list.symbolicEquals(list_0, list_1, path).guardedValues
                .getOrDefault(false, Bdd.constFalse()).isConstTrue());

        list_1 = ops_list.removeAt(list_1, var_y0).item;

        assertTrue(ops_list.symbolicEquals(list_0, list_1, path).guardedValues
                .getOrDefault(true, Bdd.constFalse()).isConstTrue());
    }

}
