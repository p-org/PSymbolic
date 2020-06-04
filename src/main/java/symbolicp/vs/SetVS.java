package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetVS<T> implements ValueSummary<SetVS<T>>{
    /* Invariant: 'size' should be consistent with 'elements', in the sense that for any assignment of concrete Bdd
     * variables, the concrete entry in 'size' whose guard is satisfied, if such an entry exists, should match the
     * number of entries in 'elements' whose guards are satisfied.
     */
    public final PrimVS<Integer> size;

    /* A key with no entry in 'elements' represents an element whose guard is identically false.
     * We should always keep 'elements' in a normalized state where no element has a guard which is identically false.
     */
    public final Map<T, Bdd> elements;

    /* Caution: Callers must take care to ensure that the above invariants are satisfied. */
    public SetVS(PrimVS<Integer> size, Map<T, Bdd> elements) {
        this.size = size;
        this.elements = elements;
    }

    public SetVS() {
        this.size = new PrimVS<>(0);
        this.elements = new HashMap<>();
    }

    @Override
    public SetVS<T> guard(Bdd cond) {
        final PrimVS<Integer> newSize = VSOps.guard(size, cond);

        final Map<T, Bdd> newElements = new HashMap<>();
        for (Map.Entry<T, Bdd> entry : elements.entrySet()) {
            final Bdd newGuard = entry.getValue().and(cond);

            if (newGuard.isConstFalse()) {
                continue;
            }

            newElements.put(entry.getKey(), newGuard);
        }

        return new SetVS<>(newSize, newElements);
    }

    @Override
    public SetVS<T> merge(SetVS<T> other) {

        final Map<T, Bdd> mergedElements = new HashMap<>();
        final PrimVS<Integer> mergedSize = size.merge(other.size);

        for (Map.Entry<T, Bdd> entry : elements.entrySet()) {
            mergedElements.merge(entry.getKey(), entry.getValue(), Bdd::or);
        }
        for (Map.Entry<T, Bdd> entry : other.elements.entrySet()) {
            mergedElements.merge(entry.getKey(), entry.getValue(), Bdd::or);
        }

        return new SetVS<>(mergedSize, mergedElements);
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(SetVS<T> other, Bdd pc) {
        Bdd equalCond = Bdd.constTrue();
        for (Map.Entry<T, Bdd> entry : elements.entrySet()) {
            /* Check common elements */
            if (other.elements.containsKey(entry.getKey())) {
                equalCond = (entry.getValue().and(other.elements.get(entry.getKey()))) //both present
                        .or(entry.getValue().or(other.elements.get(entry.getKey())).not()) //both not present
                        .and(equalCond);
            }
            /* Elements unique to left must not be present*/
            else {
                equalCond = entry.getValue().not().and(equalCond);
            }
        }
        for (Map.Entry<T, Bdd> entry : other.elements.entrySet()) {
            /* Elements unique to right must not be present*/
            if (!other.elements.containsKey(entry.getKey())) {
                equalCond = entry.getValue().not().and(equalCond);
            }
        }
        return BoolUtils.fromTrueGuard(pc.and(equalCond));
    }
}
