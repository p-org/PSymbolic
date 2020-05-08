package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.PrimVS;
import symbolicp.vs.ValueSummaryOps;

import java.util.HashMap;
import java.util.Map;

public class GotoOutcome {
    private final PrimVS.Ops<StateTag> stateOps;

    private Bdd cond;
    private PrimVS<StateTag> dest;
    private Map<StateTag, Object> payloads;

    public GotoOutcome() {
        stateOps = new PrimVS.Ops<>();

        cond = Bdd.constFalse();
        dest = stateOps.empty();
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
        dest = stateOps.merge2(dest, stateOps.guard(new PrimVS<>(newDest), pc));

        if (newPayload != null) {
            payloads.merge(newDest, newPayload, payloadOps::merge2);
        }
    }

    public void addGuardedGoto(Bdd pc, StateTag newDest) {
        addGuardedGoto(pc, newDest, null, null);
    }
}
