package symbolicp.vs;

import symbolicp.bdd.Bdd;

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
public class PairVS<Left extends ValueSummary<Left>, Right extends ValueSummary<Right>> implements ValueSummary<PairVS<Left, Right>>{
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
    public PairVS(Left left, Right right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public PairVS<Left, Right> guard(Bdd cond) {
        return new PairVS<>(VSOps.guard(left, cond), VSOps.guard(right, cond));
    }

    @Override
    public PairVS<Left, Right> merge(PairVS<Left, Right> other) {
        return new PairVS<>(VSOps.merge2(left, other.left), VSOps.merge2(right, other.right));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(PairVS<Left, Right> other, Bdd pc) {
        return BoolUtils.fromTrueGuard(VSOps.symbolicEquals(left, other.left, pc).guardedValues.get(Boolean.TRUE).and(
                VSOps.symbolicEquals(right, other.right, pc).guardedValues.get(Boolean.TRUE)));
    }

    @Deprecated
    public static class Ops<Left extends ValueSummary<Left>, Right extends ValueSummary<Right>> implements ValueSummaryOps<PairVS<Left, Right>> {
        private final ValueSummaryOps<Left> leftOps;
        private final ValueSummaryOps<Right> rightOps;

        public Ops(ValueSummaryOps<Left> leftOps, ValueSummaryOps<Right> rightOps) {
            this.leftOps = leftOps;
            this.rightOps = rightOps;
        }

        @Override
        public boolean isEmpty(PairVS<Left, Right> summary) {
            return leftOps.isEmpty(summary.left) || rightOps.isEmpty(summary.right);
        }

        @Override
        public PairVS<Left, Right> empty() {
            return new PairVS<>(leftOps.empty(), rightOps.empty());
        }

        @Override
        public PairVS<Left, Right> guard(PairVS<Left, Right> summary, Bdd guard) {
            return new PairVS<>(leftOps.guard(summary.left, guard), rightOps.guard(summary.right, guard));
        }

        @Override
        public PairVS<Left, Right> merge(Iterable<PairVS<Left, Right>> summaries) {
            final List<Left> lefts = new ArrayList<>();
            final List<Right> rights = new ArrayList<>();

            for (PairVS<Left, Right> pair : summaries) {
                lefts.add(pair.left);
                rights.add(pair.right);
            }

            return new PairVS<>(leftOps.merge(lefts), rightOps.merge(rights));
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(PairVS<Left, Right> left, PairVS<Left, Right> right, Bdd pc) {
            Bdd leftEqual = leftOps.symbolicEquals(left.left, right.left, pc).guardedValues.get(Boolean.TRUE);
            Bdd rightEqual = rightOps.symbolicEquals(left.right, right.right, pc).guardedValues.get(Boolean.TRUE);
            return BoolUtils.fromTrueGuard(leftEqual.and(rightEqual));
        }

    }
}
