package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SymbolicBag<T extends ValueSummary<T>> {

    private SetVS<T> entries;

    public SymbolicBag() {
        this.entries = new SetVS<>(Bdd.constTrue());
        assert(entries.getUniverse().isConstTrue());
    }

    public PrimVS<Integer> size() { return entries.size(); }

    public void add(T entry) {
        entries = entries.add(entry);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Bdd enabledCond() {
        return entries.getNonEmptyUniverse();
    }

    /** Get the condition under which the first queue entry obeys the provided predicate
     * @param pred The filtering predicate
     * @return The condition under which the first queue entry obeys pred
     */
    public PrimVS<Boolean> enabledCond(Function<T, PrimVS<Boolean>> pred) {
        Bdd cond = entries.getNonEmptyUniverse();
        ListVS<T> elts = entries.guard(cond).getElements();
        PrimVS<Integer> idx = new PrimVS<>(0).guard(cond);
        PrimVS<Boolean> enabledCond = new PrimVS<>(false);
        while (BoolUtils.isEverTrue(IntUtils.lessThan(idx, elts.size()))) {
            Bdd iterCond = IntUtils.lessThan(idx, elts.size()).getGuard(true);
            PrimVS<Boolean> res = pred.apply(elts.get(idx.guard(iterCond)));
            enabledCond = BoolUtils.or(enabledCond, res);
            idx = IntUtils.add(idx, 1);
        }
        return enabledCond;
    }

    public T remove(Bdd pc) {
        assert (entries.getUniverse().isConstTrue());
        ListVS<T> filtered = entries.guard(pc).getElements();
        PrimVS<Integer> size = filtered.size();
        List<PrimVS> choices = new ArrayList<>();
        for (GuardedValue<Integer> guardedValue : size.getGuardedValues()) {
            choices.add(new PrimVS<Integer>(guardedValue.value).guard(guardedValue.guard));
        }
        PrimVS<Integer> index = (PrimVS<Integer>) NondetUtil.getNondetChoice(choices);
        T element = filtered.get(index);
        entries = entries.update(pc, entries.remove(element).guard(pc));
        return element;
    }

    @Override
    public String toString() {
        return "SymbolicQueue{" +
                "entries=" + entries +
                '}';
    }
}
