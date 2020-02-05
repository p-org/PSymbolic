package symbolicp;

public interface BddLib<Bdd> {
    Bdd constFalse();

    Bdd constTrue();

    boolean isConstFalse(Bdd bdd);

    boolean isConstTrue(Bdd bdd);

    Bdd and(Bdd left, Bdd right);

    Bdd or(Bdd left, Bdd right);

    Bdd not(Bdd bdd);

    Bdd ifThenElse(Bdd cond, Bdd thenClause, Bdd elseClause);

    Bdd newVar();
}

