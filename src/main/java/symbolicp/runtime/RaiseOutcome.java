package symbolicp.runtime;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import symbolicp.bdd.Bdd;
import symbolicp.vs.EventVS;

public class RaiseOutcome<EventTag> {

    public boolean isEmpty() {
        throw new NotImplementedException();
    }

    public Bdd getRaiseCond() {
        throw new NotImplementedException();
    }

    public EventVS<EventTag> getEventSummary() {
        throw new NotImplementedException();
    }


}
