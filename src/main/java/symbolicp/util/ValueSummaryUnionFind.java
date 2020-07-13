package symbolicp.util;

import symbolicp.bdd.Bdd;
import symbolicp.runtime.ScheduleLogger;
import symbolicp.vs.PrimVS;

import java.util.*;
import java.util.function.BiPredicate;

public class ValueSummaryUnionFind extends UnionFind<PrimVS> {

    Map<PrimVS, Bdd> universe = new HashMap<>();

    public ValueSummaryUnionFind(Collection<PrimVS> c) {
        super();
        for (PrimVS elt : c) {
            addElement(elt);
            List<PrimVS> values = new ArrayList<>(parents.values());
            Bdd eltUniverse = elt.getUniverse();
            for (int i = 0; i < values.size(); i ++) {
                if (elt.equals(values.get(i))) {
                    continue;
                }
                Bdd unionUniverse = universe.get(find(values.get(i)));
                if (!eltUniverse.and(unionUniverse).isConstFalse()) {
                        union(elt, values.get(i));
                    if (eltUniverse.implies(unionUniverse).isConstTrue()) {
                        break;
                    }
                }
            }
        }
    }

    public Map<Set<PrimVS>, Bdd> getLastUniverseMap() {
        Map<Set<PrimVS>, Bdd> lastUniverseMap = new HashMap<>();
        for (Set<PrimVS> set : lastDisjointSet) {
            lastUniverseMap.put(set, universe.get(find(set.iterator().next())));
        }
        return lastUniverseMap;
    }

    public void addElement(PrimVS elt) {
        super.addElement(elt);
        universe.put(elt, elt.getUniverse());
    }

    public boolean union(PrimVS e1, PrimVS e2) {
        Bdd universe1 = universe.get(find(e1));
        Bdd universe2 = universe.get(find(e2));
        boolean res = super.union(e1, e2);
        universe.put(find(e1), universe1.or(universe2));
        return res;
    }
}
