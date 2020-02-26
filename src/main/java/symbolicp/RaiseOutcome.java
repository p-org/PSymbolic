package symbolicp;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
