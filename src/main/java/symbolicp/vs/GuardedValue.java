package symbolicp.vs;

import symbolicp.bdd.Bdd;

public class GuardedValue<T> {
    public final T value;
    public final Bdd guard;

    GuardedValue(T value, Bdd guard) {
        this.value = value;
        this.guard = guard;
    }
}
