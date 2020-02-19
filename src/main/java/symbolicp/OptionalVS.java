package symbolicp;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OptionalVS<Item> {
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

    public static class Ops<Item> implements ValueSummaryOps<OptionalVS<Item>> {
        private final ValueSummaryOps<PrimVS<Boolean>> boolOps;
        private final ValueSummaryOps<Item> itemOps;

        public Ops(ValueSummaryOps<Item> itemOps) {
            this.boolOps = new PrimVS.Ops<>();
            this.itemOps = itemOps;
        }

        @Override
        public boolean isEmpty(OptionalVS<Item> summary) {
            return boolOps.isEmpty(summary.present);
        }

        @Override
        public OptionalVS<Item> empty() {
            return new OptionalVS<>(boolOps.empty(), itemOps.empty());
        }

        @Override
        public OptionalVS<Item> guard(OptionalVS<Item> summary, Bdd guard) {
            return new OptionalVS<>(
                boolOps.guard(summary.present, guard),
                itemOps.guard(summary.item, guard)
            );
        }

        @Override
        public OptionalVS<Item> merge(Iterable<OptionalVS<Item>> summaries) {
            final List<PrimVS<Boolean>> presentFlagsToMerge = new ArrayList<>();
            final List<Item> itemsToMerge = new ArrayList<>();

            for (OptionalVS<Item> summary : summaries) {
                presentFlagsToMerge.add(summary.present);
                itemsToMerge.add(summary.item);
            }

            return new OptionalVS<>(
                boolOps.merge(presentFlagsToMerge),
                itemOps.merge(itemsToMerge)
            );
        }

        @Override
        public PrimVS<Boolean> symbolicEquals(OptionalVS<Item> left, OptionalVS<Item> right, Bdd pc) {
            Bdd bothPresent = left.present.guardedValues.get(Boolean.TRUE).and(right.present.guardedValues.get(Boolean.TRUE));
            Bdd bothAbsent = left.present.guardedValues.get(Boolean.FALSE).and(right.present.guardedValues.get(Boolean.FALSE));
            Bdd equals = itemOps.symbolicEquals(left.item, right.item, pc).guardedValues.get(Boolean.TRUE);
            return BoolUtils.fromTrueGuard(bothPresent.and(equals).or(bothAbsent).and(pc));
        }

        public OptionalVS<Item> makePresent(Item item) {
            /* TODO: Strictly speaking, here we create an optional whose 'present' tag may violate
             *  an implicit invariant by being true under more conditions than we need it
             *  to be.
             */
            return new OptionalVS<>(
                new PrimVS<>(true),
                item
            );
        }

        public OptionalVS<Item> makeAbsent() {
            /* TODO: Strictly speaking, here we create an optional whose 'present' tag may violate
             *  an implicit invariant by being false under more conditions than we need it
             *  to be.
             */
            return new OptionalVS<Item>(
                new PrimVS<>( false),
                itemOps.empty()
            );
        }

        public Item unwrapOrThrow(OptionalVS<Item> summary) {
            final @Nullable Bdd absentCond = summary.present.guardedValues.get(false);
            if (absentCond != null && !absentCond.isConstFalse()) {
                throw new BugFoundException("Attempt to unwrap an absent optional value", absentCond);
            }

            return summary.item;
        }
    }
}
