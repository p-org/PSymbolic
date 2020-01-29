package symbolicp.prototypes;
import jsylvan.JSylvan;

import java.util.*;

public class App {
    public static void main(String[] args) {
        final ListVS.Ops<PrimVS<Integer>> intListOps =
            new ListVS.Ops<>(new PrimVS.Ops<>());

        final Map<Integer, Bdd> val1Entries = new HashMap<>();
        val1Entries.put(1, new Bdd(new HashSet<>(Arrays.asList("a", "b"))));
        val1Entries.put(2, new Bdd(new HashSet<>(Arrays.asList("c", "d"))));
        final PrimVS<Integer> val1 = new PrimVS<>(val1Entries);

        final Map<Integer, Bdd> val2Entries = new HashMap<>();
        val2Entries.put(100, new Bdd(new HashSet<>(Arrays.asList("a", "c"))));
        val2Entries.put(200, new Bdd(new HashSet<>(Arrays.asList("b", "d"))));
        final PrimVS<Integer> val2 = new PrimVS<>(val2Entries);

        final PrimVS<Integer> val3 = val1.map2(val2, (x, y) -> x + y);

        System.out.println("val3: " + val3.guardedValues);

        ListVS<PrimVS<Integer>> myList = new ListVS<>();
        myList = intListOps.add(myList, val2);
        myList = intListOps.add(myList, val3);

        final OptionalVS<PrimVS<Integer>> dependentItem =
            intListOps.get(myList, val1);
        System.out.println("Dependent item present: " + dependentItem.present.guardedValues);
        System.out.println("Dependent item content: " + dependentItem.item.guardedValues);
    }
}
