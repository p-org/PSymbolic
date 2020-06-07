package symbolicp;

import org.junit.jupiter.api.Test;
import symbolicp.bdd.Bdd;
import symbolicp.vs.ListVS;
import symbolicp.vs.MapVS;
import symbolicp.vs.PrimVS;
import symbolicp.vs.SetVS;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBdd {
    @Test
    public void testPrimVS()
    {
        Bdd path = Bdd.constTrue();
        Bdd bdd1 = Bdd.newVar();
        Bdd bdd2 = Bdd.newVar();
        Bdd path2 = path.and(bdd2);
        Bdd path3 = path2.and(bdd1).or(path2.and(bdd1.not()));
        System.out.println(path3.equals(path2));
    }
}
