package symbolicp.vs;

import symbolicp.bdd.Bdd;

import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Integer.min;

public class ListVS<Item extends ValueSummary<Item>> implements ValueSummary<ListVS<Item>>{
    private final PrimVS<Integer> size;
    private final List<Item> items;

    public ListVS(PrimVS<Integer> size, List<Item> items) {
        this.size = size;
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public PrimVS<Integer> getSize() {
        return size;
    }

    public ListVS() {
        this(new PrimVS<>(0), new ArrayList<>());
    }

    @Override
    public ListVS<Item> guard(Bdd cond) {
        final PrimVS<Integer> newSize = VSOps.guard(size, cond);
        final List<Item> newItems = new ArrayList<>();

        for (Item item : items) {
            Item newItem = VSOps.guard(item, cond);
            if (VSOps.isEmpty(newItem)) {
                break; // No items after this item are possible either due to monotonicity / non-sparseness
            }
            newItems.add(newItem);
        }

        return new ListVS<>(newSize, newItems);
    }

    @Override
    public ListVS<Item> merge(ListVS<Item> other) {

        final PrimVS<Integer> mergedSize = VSOps.merge2(size, other.size);
        final List<Item> mergedItems = new ArrayList<>();

        for (int i = 0; i < min(items.size(), other.items.size()); i++) {
            mergedItems.add(VSOps.merge2(items.get(i), other.items.get(i)));
        }
        if (items.size() > other.items.size())
            for (int i = other.items.size(); i < items.size(); i++) {
                mergedItems.add(items.get(i));
            }
        else {
            for (int i = items.size(); i < other.items.size(); i++) {
                mergedItems.add(other.items.get(i));
            }
        }

        return new ListVS<>(mergedSize, mergedItems);
    }

    @Override
    public PrimVS<Boolean> symbolicEquals(ListVS<Item> other, Bdd pc) {
        Bdd equalCond = Bdd.constFalse();
        for (Map.Entry<Integer, Bdd> size : size.guardedValues.entrySet()) {
            if (other.size.guardedValues.containsKey(size.getKey())) {
                Bdd listEqual = IntStream.range(0, size.getKey())
                        .mapToObj((i) -> VSOps.symbolicEquals(items.get(i), other.items.get(i), pc).guardedValues.get(Boolean.TRUE))
                        .reduce(Bdd::and)
                        .orElse(Bdd.constTrue());
                equalCond = equalCond.or(listEqual);
            }
        }
        return BoolUtils.fromTrueGuard(pc.and(equalCond));
    }
}
