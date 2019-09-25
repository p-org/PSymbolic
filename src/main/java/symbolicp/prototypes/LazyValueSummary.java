package symbolicp.prototypes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class LazyValueSummary<Bdd, T> {
    public final Function<Bdd, T> evalWithGuard;

    public LazyValueSummary(Function<Bdd, T> evalWithGuard) {
        this.evalWithGuard = evalWithGuard;
    }

    public <U> LazyValueSummary<Bdd, U> flatMap(
        // This function need to satisfy some constraints for this to be a sensible operation.  In particular, I think
        // it needs to respect (in other words, commute with) guard operations.
        Function<T, LazyValueSummary<Bdd, U>> body
    ) {
        return new LazyValueSummary<>(
            (guard) -> body.apply(this.evalWithGuard.apply(guard)).evalWithGuard.apply(guard)
        );
    }

    public static class Ops<Bdd, T> {
        private final BddLib<Bdd> bddLib;
        private final ValueSummaryOps<Bdd, T> wrappedOps;

        public Ops(BddLib<Bdd> bddLib, ValueSummaryOps<Bdd, T> wrappedOps) {
            this.bddLib = bddLib;
            this.wrappedOps = wrappedOps;
        }

        public LazyValueSummary<Bdd, T> wrap(T wrapped) {
            return new LazyValueSummary<>((guard) -> wrappedOps.guard(wrapped, guard));
        }

        public LazyValueSummary<Bdd, T> guard(LazyValueSummary<Bdd, T> lazy, Bdd guard) {
            return new LazyValueSummary<>((otherGuard) -> lazy.evalWithGuard.apply(bddLib.and(guard, otherGuard)));
        }

        public LazyValueSummary<Bdd, T> merge(Iterable<LazyValueSummary<Bdd, T>> lazySummaries) {
            return new LazyValueSummary<>((guard) -> {
                // TODO: Avoid allocating an intermediate ArrayList here
                List<T> guarded = new ArrayList<>();
                for (LazyValueSummary<Bdd, T> lazySummary : lazySummaries) {
                    guarded.add(lazySummary.evalWithGuard.apply(guard));
                }
                return wrappedOps.merge(guarded);
            });
        }
    }
}
