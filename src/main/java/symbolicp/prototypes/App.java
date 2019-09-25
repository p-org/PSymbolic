package symbolicp.prototypes;

import java.util.*;

public class App {
    public static void main(String[] args) {
        final SetBddLib<String> bddLib = new SetBddLib<>(new HashSet<>(Arrays.asList("a", "b", "c", "d")));
        final ListValueSummary.Ops<Set<String>, PrimitiveValueSummary<Set<String>, Integer>> intListOps =
            new ListValueSummary.Ops<>(bddLib, new PrimitiveValueSummary.Ops<>(bddLib));

        final Map<Integer, Set<String>> val1Entries = new HashMap<>();
        val1Entries.put(1, new HashSet<>(Arrays.asList("a", "b")));
        val1Entries.put(2, new HashSet<>(Arrays.asList("c", "d")));
        final PrimitiveValueSummary<Set<String>, Integer> val1 = new PrimitiveValueSummary<>(val1Entries);

        final Map<Integer, Set<String>> val2Entries = new HashMap<>();
        val2Entries.put(100, new HashSet<>(Arrays.asList("a", "c")));
        val2Entries.put(200, new HashSet<>(Arrays.asList("b", "d")));
        final PrimitiveValueSummary<Set<String>, Integer> val2 = new PrimitiveValueSummary<>(val2Entries);

        final PrimitiveValueSummary<Set<String>, Integer> val3 = val1.map2(val2, bddLib, (x, y) -> x + y);

        System.out.println("val3: " + val3.guardedValues);

        ListValueSummary<Set<String>, PrimitiveValueSummary<Set<String>, Integer>> myList = new ListValueSummary<>(bddLib);
        myList = intListOps.add(myList, val2);
        myList = intListOps.add(myList, val3);

        final OptionalValueSummary<Set<String>, PrimitiveValueSummary<Set<String>, Integer>> dependentItem =
            intListOps.get(myList, val1);
        System.out.println("Dependent item present: " + dependentItem.present.guardedValues);
        System.out.println("Dependent item content: " + dependentItem.item.guardedValues);
    }
}
