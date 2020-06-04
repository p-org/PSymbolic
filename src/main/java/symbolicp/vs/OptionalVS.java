package symbolicp.vs;

import org.jetbrains.annotations.Nullable;
import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;

import java.util.ArrayList;
import java.util.List;

public class OptionalVS<Item extends ValueSummary<Item>> implements ValueSummary<OptionalVS<Item>>{
    /* Invariant: 'present' is true under exactly the conditions where 'item' exists.
     */
    public final PrimVS<Boolean> present;
    public final Item item;

    /** Caution: The caller must take care to ensure that the invariant stated above is upheld.
     */
    public OptionalVS(PrimVS<Boolean> present, Item item) {
        this.present = present;
        this.item = item;
    }

    @Override
    public OptionalVS<Item> guard(Bdd cond) {
        return new OptionalVS<>(
                VSOps.guard(present, cond),
                VSOps.guard(item, cond)
        );
    }

    @Override
    public OptionalVS<Item> merge(OptionalVS<Item> other) {
        return new OptionalVS<>(
                VSOps.merge2(present, other.present),
                VSOps.merge2(item, other.item)
        );
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(OptionalVS<Item> other, Bdd pc) {
        Bdd bothPresent = present.guardedValues.get(Boolean.TRUE).and(other.present.guardedValues.get(Boolean.TRUE));
        Bdd bothAbsent = present.guardedValues.get(Boolean.FALSE).and(other.present.guardedValues.get(Boolean.FALSE));
        Bdd equals = VSOps.symbolicEquals(item, other.item, pc).guardedValues.get(Boolean.TRUE);
        return BoolUtils.fromTrueGuard(bothPresent.and(equals).or(bothAbsent).and(pc));
    }
}
