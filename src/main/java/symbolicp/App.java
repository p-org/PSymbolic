package symbolicp;

import symbolicp.bdd.Bdd;
import symbolicp.vs.ListVS;
import symbolicp.vs.OptionalVS;
import symbolicp.vs.PrimVS;

import java.util.*;

public class App {
    public static void main(String[] args) {

        final Map<Integer, Bdd> val1Entries = new HashMap<>();
        val1Entries.put(1, new Bdd(new HashSet<>(Arrays.asList("a", "b"))));
        val1Entries.put(2, new Bdd(new HashSet<>(Arrays.asList("c", "d"))));
        final PrimVS<Integer> val1 = new PrimVS<>(val1Entries);

        final Map<Integer, Bdd> val2Entries = new HashMap<>();
        val2Entries.put(100, new Bdd(new HashSet<>(Arrays.asList("a", "c"))));
        val2Entries.put(200, new Bdd(new HashSet<>(Arrays.asList("b", "d"))));
        final PrimVS<Integer> val2 = new PrimVS<>(val2Entries);

        final PrimVS<Integer> val3 = val1.apply2(val2, (x, y) -> x + y);

        System.out.println("val3: " + val3.guardedValues);

        ListVS<PrimVS<Integer>> myList = new ListVS<>(Bdd.constFalse());
        myList = myList.add(val2);
        myList = myList.add(val3);

        final OptionalVS<PrimVS<Integer>> dependentItem =
            myList.get(val1);
        System.out.println("Dependent item present: " + dependentItem.present.guardedValues);
        System.out.println("Dependent item content: " + dependentItem.unwrapOrThrow().guardedValues);
    }
}
