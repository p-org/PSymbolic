package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.util.Checks;
import symbolicp.vs.GuardedValue;
import symbolicp.vs.ListVS;
import symbolicp.vs.PrimVS;

import java.util.*;
import java.util.function.Predicate;

public class SymbolicQueue<T extends SymbolicQueue.canGuard<T>> {

    public static interface canGuard<T> {
        public T guard(Bdd pc);
    }

    private ListVS<PrimVS<T>> entries;

    public SymbolicQueue() {
        this.entries = new ListVS<>(Bdd.constTrue());
        assert(entries.getUniverse().isConstTrue());
    }

    public PrimVS<Integer> size() { return entries.size(); }

    public void enqueueEntry(PrimVS<T> entry) {
        assert(Checks.includedIn(entry.getUniverse()));
        //Checks.check(Scheduler.schedule.singleScheduleToString(entry.getUniverse()) + System.lineSeparator() + entries,
        //        entries.size().guard(entry.getUniverse()).getValues().size() <= 1);
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
    public Bdd enabledCond(Predicate<T> pred) {
        Bdd cond = enabledCond();
        PrimVS<T> top = peek(cond);
        for (GuardedValue<T> guardedValue : top.getGuardedValues()) {
            if (!pred.test(guardedValue.value)) {
                cond = cond.and(guardedValue.guard.not());
            }
        }
        return cond;
    }

    public PrimVS<T> dequeueEntry(Bdd pc) {
        return peekOrDequeueHelper(pc, true);
    }

    public PrimVS<T> peek(Bdd pc) {
        return peekOrDequeueHelper(pc, false);
    }

    private PrimVS<T> peekOrDequeueHelper(Bdd pc, boolean dequeue) {
        assert(entries.getUniverse().isConstTrue());
        ListVS<PrimVS<T>> filtered = entries.guard(pc);
        if (!filtered.isEmpty()) {
            assert(Checks.sameUniverse(filtered.getUniverse(), filtered.getNonEmptyUniverse()));
            PrimVS<T> res = filtered.get(new PrimVS<>(0).guard(pc)).guard(pc);
            if (dequeue)
                entries = entries.update(pc, filtered.removeAt(new PrimVS<>(0).guard(pc)));
            Map<T, Bdd> newMapping = new HashMap<>();
            for (GuardedValue<T> guardedValue : res.getGuardedValues()) {
                newMapping.put(guardedValue.value.guard(guardedValue.guard), guardedValue.guard);
            }
            res = new PrimVS<>(newMapping);
            res.check();
            assert(entries.getUniverse().isConstTrue());
            return res;
        }
        return new PrimVS<>();
    }

    @Override
    public String toString() {
        return "SymbolicQueue{" +
                "entries=" + entries +
                '}';
    }
}
