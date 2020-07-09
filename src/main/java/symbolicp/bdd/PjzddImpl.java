package symbolicp.bdd;

import org.sosy_lab.pjbdd.Builders;
import org.sosy_lab.pjbdd.creator.zdd.ZDDCreator;
import org.sosy_lab.pjbdd.node.BDD;
import org.sosy_lab.pjbdd.util.parser.*;
import symbolicp.runtime.Schedule;
import symbolicp.runtime.ScheduleLogger;
import symbolicp.util.NotImplementedException;

// try keeping a root node?
// for checking if false, check if it's just the root node or not
// for updating universe with new variables, can just negate the rest of everything

public class PjzddImpl implements BddLib<BDD> {
    final private ZDDCreator c;
    final private Exporter e;
    private int count = 1;
    final private int max = 15;

    public PjzddImpl() {
        c = Builders.newZDDBuilder().setVarCount(max).build();
        e = new DotExporter();
    }

    @Override
    public BDD constFalse() {
        return c.empty();
    }

    @Override
    public BDD constTrue() {
        return c.base();
    }

    @Override
    public boolean isConstFalse(BDD bdd) {
        return bdd.isFalse();
    }

    @Override
    public boolean isConstTrue(BDD bdd) {
        return bdd.isTrue();
    }

    @Override
    public BDD and(BDD left, BDD right) {
        return c.intersection(left, right);
    }

    @Override
    public BDD or(BDD left, BDD right) {
        return c.union(left, right);
    }

    @Override
    public BDD not(BDD bdd) {
        // TODO: how
        throw new NotImplementedException();
        /*
        BDD ret;
        if (bdd.isTrue()) { ret = constFalse(); }
        else if (bdd.isFalse()) { ret = constTrue(); }
        else {
            ret = c.exclude(c.universe(), bdd);
        }
        ScheduleLogger.log("arg: " + toString(bdd));
        ScheduleLogger.log("not: " + toString(ret));
        //assert(and(ret, bdd).isFalse());
        return ret;
        */
    }

    @Override
    public BDD implies(BDD left, BDD right) {
        throw new NotImplementedException();
    }

    @Override
    public BDD ifThenElse(BDD cond, BDD thenClause, BDD elseClause) {
        return or(and(cond, thenClause), and(not(cond), elseClause));
    }

    @Override
    public BDD newVar() {
        assert(count < max);
        BDD newNode = c.makeNode(constFalse(), constTrue(), count++);
        return newNode;
    }

    @Override
    public String toString(BDD bdd) {
        if (bdd == null) return "null";
        //if (isConstFalse(bdd)) return "false";
        //if (isConstTrue(bdd)) return "true";
        return e.bddToString(bdd);
    }

    @Override
    public BDD fromString(String s) {
        throw new NotImplementedException();
    }
}
