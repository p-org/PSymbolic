package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.ValueSummary;

import java.util.*;

public class SymbolicQueue<T extends SymbolicQueue.Entry> {
    public interface Entry<T> {
        public Bdd getCond();
        public T withCond(Bdd cond);
    }

    private LinkedList<T> entries;

    LinkedList<T> getEntries () { return entries; }

    public SymbolicQueue() {
        this.entries = new LinkedList<>();
    }

    public void enqueueEntry(T entry) {
        // TODO: We could do some merging here in the future
        entries.addLast(entry);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    // TODO: Can/should we optimize this?
    public Bdd enabledCond() {
        Bdd result = Bdd.constFalse();
        for (T entry : entries) {
            result = result.or(entry.getCond());
        }
        return result;
    }

    public List<T> dequeueEntry(Bdd pc) {
        List<T> result = new ArrayList<>();

        ListIterator<T> candidateIter = entries.listIterator();
        while (candidateIter.hasNext() && !pc.isConstFalse()) {
            Entry<T> entry = candidateIter.next();
            Bdd dequeueCond = entry.getCond().and(pc);
            if (!dequeueCond.isConstFalse()) {
                Bdd remainCond = entry.getCond().and(pc.not());
                if (remainCond.isConstFalse()) {
                    candidateIter.remove();
                } else {
                    T remainingEntry = entry.withCond(remainCond);
                    candidateIter.set(remainingEntry);
                }
                result.add(entry.withCond(dequeueCond));

                // We only want to pop the first entry from the queue.  However, which entry is "first" is
                // symbolically determined.  Even if the entry we just popped was first under the path constraint
                // 'dequeueCond', there may be a "residual" path constraint under which it does not exist, and
                // therefore a different entry is first in the queue.
                pc = pc.and(entry.getCond().not());
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "SymbolicQueue{" +
                "entries=" + entries +
                '}';
    }
}
