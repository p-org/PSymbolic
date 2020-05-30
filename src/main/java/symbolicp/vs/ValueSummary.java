package symbolicp.vs;

import symbolicp.bdd.Bdd;
import symbolicp.util.NotImplementedException;

import java.util.Collections;
import java.util.List;

public interface ValueSummary<T extends ValueSummary> {
    public boolean isEmpty();
    public T guard(Bdd guard);
    public T merge(Iterable<T> summaries);
    public T update(Bdd guard, T update);
    PrimVS<Boolean> symbolicEquals(T cmp, Bdd pc);
    Bdd getUniverse();
}
