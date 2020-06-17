package symbolicp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import symbolicp.bdd.Bdd;
import symbolicp.vs.ListVS;
import symbolicp.vs.MapVS;
import symbolicp.vs.PrimVS;
import symbolicp.vs.SetVS;



public class TestValueSummaries {

    @Test
    public void testPrimVS()
    {
        Bdd path = Bdd.constTrue();
        Bdd bdd1 = Bdd.newVar();

        PrimVS<Integer> var_x = new PrimVS<>(0).guard(path);
        PrimVS<Integer> var_y = new PrimVS<>(0).guard(path);
        PrimVS<Integer> var_xg = new PrimVS<>(1).guard(bdd1);
        PrimVS<Integer> var_yg = new PrimVS<>(1).guard(bdd1.not());

        assertTrue(var_x.symbolicEquals(var_y, path).getGuard(true).isConstTrue());

        PrimVS<Integer> var_x1 = var_x.guard(bdd1.not()).merge(var_xg);
        PrimVS<Integer> var_y1 = var_y.guard(bdd1).merge(var_yg);

        assertTrue(var_x1.symbolicEquals(var_y1, path).getGuard(false).isConstTrue());
    }

    @Test
    public void testSetVS()
    {
        Bdd path = Bdd.constTrue();

        PrimVS<Integer> var_x0 = new PrimVS<>(0).guard(path);
        PrimVS<Integer> var_y0 = new PrimVS<>(0).guard(path);
        PrimVS<Integer> var_x1 = new PrimVS<>(1).guard(path);
        PrimVS<Integer> var_y1 = new PrimVS<>(1).guard(path);

        SetVS<PrimVS<Integer>> set_0 = new SetVS<>(path);
        set_0 = set_0.add(var_x0);
        set_0 = set_0.add(var_x1);

        SetVS<PrimVS<Integer>> set_1 = new SetVS<>(path);
        set_1 = set_1.add(var_y0);
        set_1 = set_1.add(var_y1);

        assertTrue(set_0.symbolicEquals(set_1, path).getGuard(true).isConstTrue());

        set_1 = set_1.remove(var_y0);

        assertTrue(set_1.symbolicEquals(set_0, path).getGuard(false).isConstTrue());
    }

    @Test
    public void testMapVS()
    {
        Bdd path = Bdd.constTrue();

        PrimVS<Integer> var_x0 = new PrimVS<>(0).guard(path);
        PrimVS<Integer> var_y0 = new PrimVS<>(0).guard(path);
        PrimVS<Integer> var_x1 = new PrimVS<>(1).guard(path);
        PrimVS<Integer> var_y1 = new PrimVS<>(1).guard(path);
        PrimVS<Integer> var_y2 = new PrimVS<>(2).guard(path);

        MapVS<Integer, PrimVS<Integer>> map_0 = new MapVS<>(Bdd.constTrue());
        MapVS<Integer, PrimVS<Integer>> map_1 = new MapVS<>(Bdd.constTrue());

        assertTrue(map_0.symbolicEquals(map_1, path).getGuard(true).isConstTrue());

        map_0 = map_0.add(var_x0, var_x1);
        map_1 = map_1.add(var_y0, var_y1);

        assertTrue(map_0.symbolicEquals(map_1, path).getGuard(true).isConstTrue());

        //map_0 = ops_map.add(map_0, var_y2, var_x0);
        map_1 = map_1.add(var_y2, var_y1);
        assertTrue(map_0.symbolicEquals(map_1, path).getGuard(false).isConstTrue());
    }

    @Test
    public void testListVS()
    {
        Bdd path = Bdd.constTrue();

        PrimVS<Integer> var_x0 = new PrimVS<>(0).guard(path);
        PrimVS<Integer> var_y0 = new PrimVS<>(0).guard(path);
        PrimVS<Integer> var_x1 = new PrimVS<>(1).guard(path);
        PrimVS<Integer> var_y1 = new PrimVS<>(1).guard(path);
        PrimVS<Integer> var_y2 = new PrimVS<>(2).guard(path);
        PrimVS<Integer> var_x2 = new PrimVS<>(2).guard(path);
        PrimVS<Integer> var_y3 = new PrimVS<>(2).guard(path);

        ListVS<PrimVS<Integer>> list_0 = new ListVS<>(path);
        ListVS<PrimVS<Integer>> list_1 = new ListVS<>(path);

        assertTrue(list_0.symbolicEquals(list_1, path).getGuard(true).isConstTrue());

        list_0 = list_0.add(var_x0);
        list_0 = list_0.add(var_x1);
        list_0 = list_0.add(var_x2);

        list_1 = list_1.add(var_y3);
        list_1 = list_1.add(var_y0);
        list_1 = list_1.add(var_y1);
        list_1 = list_1.add(var_y2);

        assertTrue(list_0.symbolicEquals(list_1, path).getGuard(false).isConstTrue());

        list_1 = list_1.removeAt(var_y0);

        assertTrue(list_0.symbolicEquals(list_1, path).getGuard(true).isConstTrue());
    }

}
