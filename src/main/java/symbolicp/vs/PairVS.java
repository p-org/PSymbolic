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
}
