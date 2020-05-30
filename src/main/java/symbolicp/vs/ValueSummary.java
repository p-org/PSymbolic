package symbolicp.vs;

import symbolicp.bdd.Bdd;
import symbolicp.runtime.TypeTag;

public interface ValueSummary<T extends ValueSummary> {

    /**
     * Casts an AnyVS (UnionVS<TypeTag>) to a concrete Value Summary type. If there is some non
     * constantly false path constraint under which the current pc is defined but not the guard
     * corresponding to the specified type, the function throws a ClassCastException.
     */
    default ValueSummary fromAny(Bdd pc, TypeTag typeTag, UnionVS<TypeTag> src) {
        Bdd typeGuard = src.getTag().getGuard(typeTag);
        Bdd pcNotDefined = pc.and(typeGuard.not());
        if (!pcNotDefined.isConstFalse()) {
            throw new ClassCastException(String.format("Symbolic casting under path constraint %s is not defined",
                    pcNotDefined));
        }
        return ((ValueSummary) src.getPayload(typeTag)).guard(pc);
    }

    public boolean isEmpty();
    public T guard(Bdd guard);
    public T merge(Iterable<T> summaries);
    public T merge(T summary);
    public T update(Bdd guard, T update);
    PrimVS<Boolean> symbolicEquals(T cmp, Bdd pc);
    Bdd getUniverse();
}
