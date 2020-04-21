package symbolicp.bdd;

import org.sosy_lab.pjbdd.Builders;
import org.sosy_lab.pjbdd.creator.bdd.Creator;
import org.sosy_lab.pjbdd.node.BDD;

public class PjbddImpl implements BddLib<BDD> {
    final private Creator c;

    public PjbddImpl() {
        c = Builders.newBDDBuilder().build();
    }

    @Override
    public BDD constFalse() {
        return c.makeFalse();
    }

    @Override
    public BDD constTrue() {
        return c.makeTrue();
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
        return c.makeAnd(left, right);
    }

    @Override
    public BDD or(BDD left, BDD right) {
        return c.makeOr(left, right);
    }

    @Override
    public BDD not(BDD bdd) {
        return c.makeNot(bdd);
    }

    @Override
    public BDD ifThenElse(BDD cond, BDD thenClause, BDD elseClause) {
        return c.makeIte(cond, thenClause, elseClause);
    }

    @Override
    public BDD newVar() {
        return c.makeVariable();
    }
}
