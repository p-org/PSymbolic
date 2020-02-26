package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.Arrays;

public interface ValueSummaryOps<ValueSummary> {
    /** Informally, a value summary is "empty" if it contains no values (or, equivalently, if all of its values are
     * guarded by conditions which are identically false).
     */
    boolean isEmpty(ValueSummary summary);

    ValueSummary empty();

    /** The guard of a value summary is always defined.
     *
     * Guarding a value summary by a condition which is identically false should always return an empty value summary
     * (one for which isEmpty(...) returns true).
     *
     * Guarding a value summary by a condition which is identically true should always return a value summary equivalent
     * to the one passed in.
     *
     * Guarding a value summary by a Bdd 'cond1' and then by a Bdd 'cond2' should be equivalent to guarding by
     * 'and(cond1, cond2)'.
     */
    ValueSummary guard(ValueSummary summary, Bdd guard);

    /** Caution: The merge of zero or more value summaries is only defined if the value summaries are pairwise "disjoint"!
     *
     * Callers must take care to ensure this precondition is always upheld.
     *
     * Formally, we consider two value summaries 'summary1' and 'summary2' to be "disjoint" if there exists a Bdd 'cond'
     * such that 'guard(summary1, cond)' is equivalent to 'summary1', and 'guard(summary2, cond)' is empty.
     *
     * guard should distribute over merge, in the sense that
     *
     *      guard(merge(summary1, summary2, ... summaryN), cond)
     *
     * should return a value summary equivalent to
     *
     *      merge(guard(summary1, cond), guard(summary2, cond), ..., guard(summaryN, cond))
     *
     * Moreover, guard should distribute over disjunctions, in the sense that if 'cond1' and 'cond2' are mutually
     * exclusive Bdds (Bdds for which 'and(cond1, cond2)' is identically false), the call
     *
     *      guard(summary1, or(cond1, cond2))
     *
     * should return a value summary equivalent to
     *
     *      merge(guard(summary1, cond1), guard(summary2, cond2))
     */
    ValueSummary merge(Iterable<ValueSummary> summaries);

    /** Computes equality of two value summaries and return
     *
     *
     *
     */
    PrimVS<Boolean> symbolicEquals(ValueSummary left, ValueSummary right, Bdd pc);

    default ValueSummary merge2(ValueSummary summary1, ValueSummary summary2) {
        return merge(Arrays.asList(summary1, summary2));
    }
}