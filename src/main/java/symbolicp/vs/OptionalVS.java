package symbolicp.vs;

import org.jetbrains.annotations.Nullable;
import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Class for option value summaries */
public class OptionalVS<T extends ValueSummary<T>> implements ValueSummary<OptionalVS<T>> {
    /** Invariant: 'present' is true under exactly the conditions where 'item' exists. */
    public final PrimVS<Boolean> present;
    /** The item that may or not be present */
    private final T item;

    /** Caution: The caller must take care to ensure that the invariant stated above is upheld. */
    private OptionalVS(PrimVS<Boolean> present, T item) {
        this.present = present;
        this.item = item;
    }

    /** Make a new OptionalVS with item present under the conditions that it exists.
     * @param item The item, which is present.
     */
    public OptionalVS(T item) {
        this(BoolUtils.fromTrueGuard(item.getUniverse()), item);
    }

    /** Make a new empty OptionalVS under the specified universe.
     * @param universe The universe for the new OptionalVS
     */
    public OptionalVS(Bdd universe) { this(new PrimVS<>(false).guard(universe), null); }

    /** Make a new empty OptionalVS under the largest possible universe (true). */
    public OptionalVS() {
        this(new PrimVS<>(false), null);
    }

    @Override
    public Bdd getUniverse() {
        return present.getUniverse();
    }

    @Override
    public boolean isEmptyVS() {
        return present.isEmptyVS();
    }

    @Override
    public OptionalVS<T> guard(Bdd guard) {
        if (item == null) {
            return new OptionalVS<T>(
                    present.guard(guard),
                    null);
        }
        return new OptionalVS<T>(
                present.guard(guard),
                item.guard(guard)
        );
    }

    @Override
    public OptionalVS<T> update(Bdd guard, OptionalVS<T> update) {
        return this.guard(guard.not()).merge(Collections.singletonList(update.guard(guard)));
    }

    @Override
    public OptionalVS<T> merge(Iterable<OptionalVS<T>> summaries) {
        final List<PrimVS<Boolean>> presentFlagsToMerge = new ArrayList<>();
        final List<T> itemsToMerge = new ArrayList<>();

        T mergeItem = this.item;
        for (OptionalVS<T> summary : summaries) {
            if (summary.item != null) {
                if (this.item == null) mergeItem = summary.item;
                presentFlagsToMerge.add(summary.present);
                itemsToMerge.add(summary.item);
            }
        }

        if (mergeItem == null) return new OptionalVS<T>();

        return new OptionalVS<T>(
                this.present.merge(presentFlagsToMerge),
                mergeItem.merge(itemsToMerge)
        );
    }

    @Override
    public OptionalVS<T> merge(OptionalVS<T> summary) {
        return merge(Collections.singletonList(summary));
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(OptionalVS<T> cmp, Bdd pc) {
        Bdd bothPresent = this.present.getGuard(Boolean.TRUE).and(cmp.present.getGuard(Boolean.TRUE));
        Bdd bothAbsent = this.present.getGuard(Boolean.FALSE).and(cmp.present.getGuard(Boolean.FALSE));
        Bdd equals = Bdd.constFalse();
        if (this.item == null) {
            if (cmp.item == null) {
                equals = Bdd.constTrue();
            }
        } else {
            equals = this.item.symbolicEquals(cmp.item, pc).getGuard(Boolean.TRUE);
        }
        return BoolUtils.fromTrueGuard(bothPresent.and(equals).or(bothAbsent).and(pc));
    }

    /** Try to unwrap the OptionalVS, or else throw an exception if the OptionalVS is empty
     * @return The value inside the OptionalVS
     */
    public T unwrapOrThrow() {
        final @Nullable Bdd absentCond = present.getGuard(false);
        if ((absentCond != null && absentCond.isConstFalse()) || item == null) {
            throw new BugFoundException("Attempt to unwrap an absent optional value", absentCond);
        }
        return item;
    }
}
