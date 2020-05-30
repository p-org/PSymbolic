package symbolicp.vs;

import jdk.internal.util.xml.impl.Pair;
import org.checkerframework.checker.nullness.Opt;
import org.graalvm.compiler.nodes.calc.IntegerDivRemNode;
import org.jetbrains.annotations.Nullable;
import symbolicp.bdd.Bdd;
import symbolicp.bdd.BugFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OptionalVS<Item extends ValueSummary<Item>> implements ValueSummary<OptionalVS<Item>> {
    /** Invariant: 'present' is true under exactly the conditions where 'item' exists. */
    public final PrimVS<Boolean> present;
    private final Item item;

    /** Caution: The caller must take care to ensure that the invariant stated above is upheld. */
    private OptionalVS(PrimVS<Boolean> present, Item item) {
        this.present = present;
        this.item = item;
    }

    /** Make a new OptionalVS with item present under the conditions that it exists.
     *
     * @param item The item, which is present.
     */
    public OptionalVS(Item item) {
        this(BoolUtils.fromTrueGuard(item.getUniverse()), item);
    }

    public OptionalVS(Bdd universe) { this(new PrimVS<>(false).guard(universe), null); }

    public OptionalVS() {
        this(new PrimVS<>(false), null);
    }

    @Override
    public Bdd getUniverse() {
        return present.getUniverse();
    }

    @Override
    public boolean isEmpty() {
        return present.isEmpty();
    }

    @Override
    public OptionalVS<Item> guard(Bdd guard) {
        if (item == null) {
            return new OptionalVS<Item>(
                    present.guard(guard),
                    null);
        }
        return new OptionalVS<Item>(
                present.guard(guard),
                item.guard(guard)
        );
    }

    @Override
    public OptionalVS<Item> update(Bdd guard, OptionalVS<Item> update) {
        return this.guard(guard.not()).merge(Collections.singletonList(update.guard(guard)));
    }

    @Override
    public OptionalVS<Item> merge(Iterable<OptionalVS<Item>> summaries) {
        final List<PrimVS<Boolean>> presentFlagsToMerge = new ArrayList<>();
        final List<Item> itemsToMerge = new ArrayList<>();

        Item mergeItem = this.item;
        for (OptionalVS<Item> summary : summaries) {
            if (summary.item != null) {
                if (this.item == null) mergeItem = summary.item;
                presentFlagsToMerge.add(summary.present);
                itemsToMerge.add(summary.item);
            }
        }

        if (mergeItem == null) return new OptionalVS<Item>();

        return new OptionalVS<Item>(
                this.present.merge(presentFlagsToMerge),
                mergeItem.merge(itemsToMerge)
        );
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(OptionalVS<Item> cmp, Bdd pc) {
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

    public Item unwrapOrThrow() {
        final @Nullable Bdd absentCond = present.getGuard(false);
        if ((absentCond != null && !absentCond.isConstFalse()) || item == null) {
            throw new BugFoundException("Attempt to unwrap an absent optional value", absentCond);
        }
        return item;
    }
}
