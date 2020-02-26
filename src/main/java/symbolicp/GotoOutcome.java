package symbolicp;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
