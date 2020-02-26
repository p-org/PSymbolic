package symbolicp.runtime;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import symbolicp.bdd.Bdd;
import symbolicp.vs.PrimVS;

public class GotoOutcome <StateTag> {

    public boolean isEmpty() {
        throw new NotImplementedException();
    }

    public Bdd getGotoCond() {
        throw new NotImplementedException();
    }

    public PrimVS<StateTag> getStateSummary() {
        throw new NotImplementedException();
    }
}
