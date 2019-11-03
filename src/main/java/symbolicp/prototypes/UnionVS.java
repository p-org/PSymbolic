package symbolicp.prototypes;

import java.util.ArrayList;
import java.util.List;

public class UnionVS<Bdd, Left, Right> {
    /* Invariant: 'left' and 'right' must be "possible under disjoint conditions."
     *
     * Invariant: 'tag' is true under exactly the conditions where 'left' exists, and false under exactly the conditions
     * where 'right' exists.
     */
    public final PrimVS<Bdd, Boolean> tag;
    public final Left left;
    public final Right right;

    /* Caution: The caller must take care to ensure that the invariants stated above are upheld.
     */
    public UnionVS(PrimVS<Bdd, Boolean> tag, Left left, Right right) {
        this.tag = tag;
        this.left = left;
        this.right = right;
    }

    public static class Ops<Bdd, T, U> implements ValueSummaryOps<Bdd, UnionVS<Bdd, T, U>> {
        private final BddLib<Bdd> bddLib;
        private final ValueSummaryOps<Bdd, PrimVS<Bdd, Boolean>> boolOps;
        private final ValueSummaryOps<Bdd, T> leftOps;
        private final ValueSummaryOps<Bdd, U> rightOps;

        public Ops(BddLib<Bdd> bddLib, ValueSummaryOps<Bdd, T> leftOps, ValueSummaryOps<Bdd, U> rightOps) {
            this.bddLib = bddLib;
            this.boolOps = new PrimVS.Ops<>(bddLib);
            this.leftOps = leftOps;
            this.rightOps = rightOps;
        }

        @Override
        public boolean isEmpty(UnionVS<Bdd, T, U> summary) {
            return boolOps.isEmpty(summary.tag);
        }

        @Override
        public UnionVS<Bdd, T, U> empty() {
            return new UnionVS<>(boolOps.empty(), leftOps.empty(), rightOps.empty());
        }

        @Override
        public UnionVS<Bdd, T, U> guard(UnionVS<Bdd, T, U> summary, Bdd guard) {
            return new UnionVS<>(
                boolOps.guard(summary.tag, guard),
                leftOps.guard(summary.left, guard),
                rightOps.guard(summary.right, guard)
            );
        }

        @Override
        public UnionVS<Bdd, T, U> merge(Iterable<UnionVS<Bdd, T, U>> summaries) {
            final List<PrimVS<Bdd, Boolean>> tagsToMerge = new ArrayList<>();
            final List<T> leftsToMerge = new ArrayList<>();
            final List<U> rightsToMerge = new ArrayList<>();

            for (UnionVS<Bdd, T, U> summary : summaries) {
                tagsToMerge.add(summary.tag);
                leftsToMerge.add(summary.left);
                rightsToMerge.add(summary.right);
            }

            return new UnionVS<>(
                boolOps.merge(tagsToMerge),
                leftOps.merge(leftsToMerge),
                rightOps.merge(rightsToMerge)
            );
        }
    }
}
