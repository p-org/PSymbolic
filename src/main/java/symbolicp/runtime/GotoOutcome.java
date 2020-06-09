package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummary;

import java.util.HashMap;
import java.util.Map;

public class GotoOutcome {

    private Bdd cond;
    private PrimVS<State> dest;
    private Map<State, ValueSummary> payloads;

    public GotoOutcome() {

        cond = Bdd.constFalse();
        dest = new PrimVS<>();
        payloads = new HashMap<>();
    }

    public boolean isEmpty() {
        return cond.isConstFalse();
    }

    public Bdd getGotoCond() {
        return cond;
    }

    public PrimVS<State> getStateSummary() {
        return dest;
    }

    public Map<State, ValueSummary> getPayloads() {
        return payloads;
    }

    public void addGuardedGoto(Bdd pc, State newDest, ValueSummary newPayload) {
        cond = cond.or(pc);
        dest = dest.merge(new PrimVS<>(newDest).guard(pc));

        if (newPayload != null) {
            payloads.merge(newDest, newPayload, (x, y) -> x.merge(y));
        }
    }

    public void addGuardedGoto(Bdd pc, State newDest) {
        addGuardedGoto(pc, newDest, null);
    }
}
