package symbolicp.bdd;

import org.sosy_lab.pjbdd.Builders;
import org.sosy_lab.pjbdd.creator.zdd.ZDDCreator;
import org.sosy_lab.pjbdd.node.BDD;
import org.sosy_lab.pjbdd.util.parser.*;
import symbolicp.util.NotImplementedException;

public class PjzbddImpl implements BddLib<BDD> {
    final private ZDDCreator c;
    final private Exporter e;
    private int count = 0;

    public PjzbddImpl() {
        c = Builders.newZDDBuilder().build();
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
        return c.union(left, right);
    }

    @Override
    public BDD or(BDD left, BDD right) {
        return c.intersection(left, right);
    }

    @Override
    public BDD not(BDD bdd) {
        return c.exclude(constTrue(), bdd);
    }

    @Override
    public BDD ifThenElse(BDD cond, BDD thenClause, BDD elseClause) {
        return or(and(cond, thenClause), and(not(cond), elseClause));
    }

    @Override
    public BDD newVar() {
        System.out.println("Var count: " + count);
        return c.makeNode(constFalse(), constTrue(), count++);
    }

    @Override
    public String toString(BDD bdd) {
        if (bdd == null) return "null";
        if (bdd.isFalse()) return "false";
        if (bdd.isTrue()) return "true";
        return e.bddToString(bdd);
    }

    @Override
    public BDD fromString(String s) {
        throw new NotImplementedException();
    }
}
