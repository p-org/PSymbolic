package symbolicp.prototypes;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OptionalVS<Bdd, Item> {
    /* Invariant: 'present' is true under exactly the conditions where 'item' exists.
     */
    public final PrimVS<Bdd, Boolean> present;
    public final Item item;

    /** Caution: The caller must take care to ensure that the invariant stated above is upheld.
     */
    public OptionalVS(PrimVS<Bdd, Boolean> present, Item item) {
        this.present = present;
        this.item = item;
    }

    public static class Ops<Bdd, Item> implements ValueSummaryOps<Bdd, OptionalVS<Bdd, Item>> {
        private final BddLib<Bdd> bddLib;
        private final ValueSummaryOps<Bdd, PrimVS<Bdd, Boolean>> boolOps;
        private final ValueSummaryOps<Bdd, Item> itemOps;

        public Ops(BddLib<Bdd> bddLib, ValueSummaryOps<Bdd, Item> itemOps) {
            this.bddLib = bddLib;
            this.boolOps = new PrimVS.Ops<>(bddLib);
            this.itemOps = itemOps;
        }

        @Override
        public boolean isEmpty(OptionalVS<Bdd, Item> summary) {
            return boolOps.isEmpty(summary.present);
        }

        @Override
        public OptionalVS<Bdd, Item> empty() {
            return new OptionalVS<>(boolOps.empty(), itemOps.empty());
        }

        @Override
        public OptionalVS<Bdd, Item> guard(OptionalVS<Bdd, Item> summary, Bdd guard) {
            return new OptionalVS<>(
                boolOps.guard(summary.present, guard),
                itemOps.guard(summary.item, guard)
            );
        }

        @Override
        public OptionalVS<Bdd, Item> merge(Iterable<OptionalVS<Bdd, Item>> summaries) {
            final List<PrimVS<Bdd, Boolean>> presentFlagsToMerge = new ArrayList<>();
            final List<Item> itemsToMerge = new ArrayList<>();

            for (OptionalVS<Bdd, Item> summary : summaries) {
                presentFlagsToMerge.add(summary.present);
                itemsToMerge.add(summary.item);
            }

            return new OptionalVS<>(
                boolOps.merge(presentFlagsToMerge),
                itemOps.merge(itemsToMerge)
            );
        }

        public OptionalVS<Bdd, Item> makePresent(Item item) {
            /* TODO: Strictly speaking, here we create an optional whose 'present' tag may violate
             *  an implicit invariant by being true under more conditions than we need it
             *  to be.
             */
            return new OptionalVS<>(
                new PrimVS<>(bddLib, true),
                item
            );
        }

        public OptionalVS<Bdd, Item> makeAbsent() {
            /* TODO: Strictly speaking, here we create an optional whose 'present' tag may violate
             *  an implicit invariant by being false under more conditions than we need it
             *  to be.
             */
            return new OptionalVS<Bdd, Item>(
                new PrimVS<>(bddLib, false),
                itemOps.empty()
            );
        }

        public Item unwrapOrThrow(OptionalVS<Bdd, Item> summary) {
            final @Nullable Bdd absentCond = summary.present.guardedValues.get(false);
            if (absentCond != null && !bddLib.isConstFalse(absentCond)) {
                throw new BugFoundException("Attempt to unwrap an optional value", absentCond);
            }

            return summary.item;
        }
    }
}
