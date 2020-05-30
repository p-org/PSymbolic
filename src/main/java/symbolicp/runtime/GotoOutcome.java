package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummaryOps;

import java.util.HashMap;
import java.util.Map;

public class GotoOutcome {

    private Bdd cond;
    private PrimVS<StateTag> dest;
    private Map<StateTag, Object> payloads;

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

    public PrimVS<StateTag> getStateSummary() {
        return dest;
    }

    public Map<StateTag, Object> getPayloads() {
        return payloads;
    }

    public void addGuardedGoto(Bdd pc, StateTag newDest, ValueSummaryOps payloadOps, Object newPayload) {
        cond = cond.or(pc);
        dest = dest.merge(new PrimVS<>(newDest).guard(pc));

        if (newPayload != null) {
            payloads.merge(newDest, newPayload, payloadOps::merge2);
        }
    }

    public void addGuardedGoto(Bdd pc, StateTag newDest) {
        addGuardedGoto(pc, newDest, null, null);
    }
}
