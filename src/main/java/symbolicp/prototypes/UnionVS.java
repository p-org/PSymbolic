package symbolicp.prototypes;

import java.util.ArrayList;
import java.util.List;

public class UnionVS<Left, Right> {
    /* Invariant: 'left' and 'right' must be "possible under disjoint conditions."
     *
     * Invariant: 'tag' is true under exactly the conditions where 'left' exists, and false under exactly the conditions
     * where 'right' exists.
     */
    public final PrimVS<Boolean> tag;
    public final Left left;
    public final Right right;

    /* Caution: The caller must take care to ensure that the invariants stated above are upheld.
     */
    public UnionVS(PrimVS<Boolean> tag, Left left, Right right) {
        this.tag = tag;
        this.left = left;
        this.right = right;
    }

    public static class Ops<T, U> implements ValueSummaryOps<UnionVS<T, U>> {
        private final ValueSummaryOps<PrimVS<Boolean>> boolOps;
        private final ValueSummaryOps<T> leftOps;
        private final ValueSummaryOps<U> rightOps;

        public Ops(ValueSummaryOps<T> leftOps, ValueSummaryOps<U> rightOps) {
            this.boolOps = new PrimVS.Ops<>();
            this.leftOps = leftOps;
            this.rightOps = rightOps;
        }

        @Override
        public boolean isEmpty(UnionVS<T, U> summary) {
            return boolOps.isEmpty(summary.tag);
        }

        @Override
        public UnionVS<T, U> empty() {
            return new UnionVS<>(boolOps.empty(), leftOps.empty(), rightOps.empty());
        }

        @Override
        public UnionVS<T, U> guard(UnionVS<T, U> summary, Bdd guard) {
            return new UnionVS<>(
                boolOps.guard(summary.tag, guard),
                leftOps.guard(summary.left, guard),
                rightOps.guard(summary.right, guard)
            );
        }

        @Override
        public UnionVS<T, U> merge(Iterable<UnionVS<T, U>> summaries) {
            final List<PrimVS<Boolean>> tagsToMerge = new ArrayList<>();
            final List<T> leftsToMerge = new ArrayList<>();
            final List<U> rightsToMerge = new ArrayList<>();

            for (UnionVS<T, U> summary : summaries) {
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
