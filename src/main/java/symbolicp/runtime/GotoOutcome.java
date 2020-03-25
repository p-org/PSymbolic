package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.PrimVS;

public class GotoOutcome {
    private final PrimVS.Ops<StateTag> stateOps;

    private Bdd cond;
    private PrimVS<StateTag> dest;

    public GotoOutcome() {
        stateOps = new PrimVS.Ops<>();

        cond = Bdd.constFalse();
        dest = stateOps.empty();
    }

    public boolean isEmpty() {
        return cond.isConstFalse();
    }

    public Bdd getGotoCond() {
        return cond;
    }

    public PrimVS<StateTag> getStateSummary() {
        return dest;
    }

    public void addGuardedGoto(Bdd pc, StateTag newDest) {
        cond = cond.or(pc);
        dest = stateOps.merge2(dest, stateOps.guard(new PrimVS<>(newDest), pc));
    }
}
