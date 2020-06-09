package symbolicp.vs;

import symbolicp.bdd.Bdd;

public interface ValueSummary<T extends ValueSummary> {

    /**
     * Casts an AnyVS (UnionVS<TypeTag>) to a concrete Value Summary type. If there is some non
     * constantly false path constraint under which the current pc is defined but not the guard
     * corresponding to the specified type, the function throws a ClassCastException.
     */
    default ValueSummary fromAny(Bdd pc, Class<? extends ValueSummary> type, UnionVS src) {
        Bdd typeGuard = src.getType().getGuard(type);
        Bdd pcNotDefined = pc.and(typeGuard.not());
        if (!pcNotDefined.isConstFalse()) {
            throw new ClassCastException(String.format("Symbolic casting under path constraint %s is not defined",
                    pcNotDefined));
        }
        return ((ValueSummary) src.getPayload(type)).guard(pc);
    }

    /** Check whether a value summary has any values under any path condition
     * @return Whether the path condition is empty
     */
    public boolean isEmptyVS();

    /** Restrict the value summary's universe with a provided guard
     * @param guard The guard to conjoin to the current value summary's universe
     * @return The result of restricting the value summary's universe
     */
    public T guard(Bdd guard);

    /** Merge the value summary with other provided value summaries
     * @param summaries The summaries to merge the value summary with
     * @return The result of the merging
     */
    public T merge(Iterable<T> summaries);

    /** Merge the value summary with another provided value summary
     * @param summary The summary to merge the value summary with
     * @return The result of the merging
     */
    public T merge(T summary);

    /** Update the value summary under the condition that the guard is true
     * @param guard The condition under which the value summary should be updated
     * @param update The value to update the value summary to
     * @return Thee result of the update
     */
    public T update(Bdd guard, T update);

    /** Check whether the value summary is equal to another value summary
     * @param cmp The summary to compare the value summary with
     * @param pc The path condition for the universe of the result
     * @return Whether or not the value summaries are equal
     */
    PrimVS<Boolean> symbolicEquals(T cmp, Bdd pc);

    /** Get the universe of the value summary
     * @return The value summary's universe
     */
    Bdd getUniverse();
}
