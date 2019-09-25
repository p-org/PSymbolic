package symbolicp.prototypes;

import java.util.ArrayList;
import java.util.List;

/** This exists mainly for demonstration purposes; in most cases we would want a tuple with more than two elements.
 *
 * To implement tuples with more than two elements, we can either...
 * 1. Easily generalize this to all tuples up to some fixed size (i.e. create classes 'Tuple3ValueSummary',
 * 'Tuple4ValueSummary', ... 'TupleNValueSummary')
 * 2. Synthesize tuples of arbitrary size during codegen in the P compiler.
 * 3. Sacrifice some type safety and create a single N-ary tuple class over dynamically typed value summaries.
 */
public class PairValueSummary<Left, Right> {
    /* Invariant: 'left' and 'right' should be "possible under the same conditions."
     *
     * Formally, for any Bdd 'cond' we should have
     *
     *      guard(cond, left) === left
     *
     * if and only if we also have
     *
     *      guard(cond, right) === right
     */

    public final Left left;
    public final Right right;

    /** Caution: The caller must take care to ensure the invariants described above are upheld. */
    public PairValueSummary(Left left, Right right) {
        this.left = left;
        this.right = right;
    }

    public static class Ops<Bdd, Left, Right> implements ValueSummaryOps<Bdd, PairValueSummary<Left, Right>> {
        private final ValueSummaryOps<Bdd, Left> leftOps;
        private final ValueSummaryOps<Bdd, Right> rightOps;

        public Ops(ValueSummaryOps<Bdd, Left> leftOps, ValueSummaryOps<Bdd, Right> rightOps) {
            this.leftOps = leftOps;
            this.rightOps = rightOps;
        }

        @Override
        public boolean isEmpty(PairValueSummary<Left, Right> summary) {
            return leftOps.isEmpty(summary.left) || rightOps.isEmpty(summary.right);
        }

        @Override
        public PairValueSummary<Left, Right> empty() {
            return new PairValueSummary<>(leftOps.empty(), rightOps.empty());
        }

        @Override
        public PairValueSummary<Left, Right> guard(PairValueSummary<Left, Right> summary, Bdd guard) {
            return new PairValueSummary<>(leftOps.guard(summary.left, guard), rightOps.guard(summary.right, guard));
        }

        @Override
        public PairValueSummary<Left, Right> merge(Iterable<PairValueSummary<Left, Right>> summaries) {
            final List<Left> lefts = new ArrayList<>();
            final List<Right> rights = new ArrayList<>();

            for (PairValueSummary<Left, Right> pair : summaries) {
                lefts.add(pair.left);
                rights.add(pair.right);
            }

            return new PairValueSummary<>(leftOps.merge(lefts), rightOps.merge(rights));
        }
    }
}
