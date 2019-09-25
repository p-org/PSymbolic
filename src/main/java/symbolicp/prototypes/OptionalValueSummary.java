package symbolicp.prototypes;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OptionalValueSummary<Bdd, Item> {
    /* Invariant: 'present' is true under exactly the conditions where 'item' exists.
     */
    public final PrimitiveValueSummary<Bdd, Boolean> present;
    public final Item item;

    /** Caution: The caller must take care to ensure that the invariant stated above is upheld.
     */
    public OptionalValueSummary(PrimitiveValueSummary<Bdd, Boolean> present, Item item) {
        this.present = present;
        this.item = item;
    }

    public static class Ops<Bdd, Item> implements ValueSummaryOps<Bdd, OptionalValueSummary<Bdd, Item>> {
        private final BddLib<Bdd> bddLib;
        private final ValueSummaryOps<Bdd, PrimitiveValueSummary<Bdd, Boolean>> boolOps;
        private final ValueSummaryOps<Bdd, Item> itemOps;

        public Ops(BddLib<Bdd> bddLib, ValueSummaryOps<Bdd, Item> itemOps) {
            this.bddLib = bddLib;
            this.boolOps = new PrimitiveValueSummary.Ops<>(bddLib);
            this.itemOps = itemOps;
        }

        @Override
        public boolean isEmpty(OptionalValueSummary<Bdd, Item> summary) {
            return boolOps.isEmpty(summary.present);
        }

        @Override
        public OptionalValueSummary<Bdd, Item> empty() {
            return new OptionalValueSummary<>(boolOps.empty(), itemOps.empty());
        }

        @Override
        public OptionalValueSummary<Bdd, Item> guard(OptionalValueSummary<Bdd, Item> summary, Bdd guard) {
            return new OptionalValueSummary<>(
                boolOps.guard(summary.present, guard),
                itemOps.guard(summary.item, guard)
            );
        }

        @Override
        public OptionalValueSummary<Bdd, Item> merge(Iterable<OptionalValueSummary<Bdd, Item>> summaries) {
            final List<PrimitiveValueSummary<Bdd, Boolean>> presentFlagsToMerge = new ArrayList<>();
            final List<Item> itemsToMerge = new ArrayList<>();

            for (OptionalValueSummary<Bdd, Item> summary : summaries) {
                presentFlagsToMerge.add(summary.present);
                itemsToMerge.add(summary.item);
            }

            return new OptionalValueSummary<>(
                boolOps.merge(presentFlagsToMerge),
                itemOps.merge(itemsToMerge)
            );
        }

        public OptionalValueSummary<Bdd, Item> makePresent(Item item) {
            /* TODO: Strictly speaking, here we create an optional whose 'present' tag may violate
             *  an implicit invariant by being true under more conditions than we need it
             *  to be.
             */
            return new OptionalValueSummary<>(
                new PrimitiveValueSummary<>(bddLib, true),
                item
            );
        }

        public OptionalValueSummary<Bdd, Item> makeAbsent() {
            /* TODO: Strictly speaking, here we create an optional whose 'present' tag may violate
             *  an implicit invariant by being false under more conditions than we need it
             *  to be.
             */
            return new OptionalValueSummary<Bdd, Item>(
                new PrimitiveValueSummary<>(bddLib, false),
                itemOps.empty()
            );
        }

        public Item unwrapOrThrow(OptionalValueSummary<Bdd, Item> summary) {
            final @Nullable Bdd absentCond = summary.present.guardedValues.get(false);
            if (absentCond != null && !bddLib.isConstFalse(absentCond)) {
                throw new BugFoundException("Attempt to unwrap an optional value", absentCond);
            }

            return summary.item;
        }
    }
}
