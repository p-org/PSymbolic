package symbolicp.bdd;

import symbolicp.util.NotImplementedException;
import java.util.HashSet;
import java.util.Set;

/** This is a mock BddLib implementation for testing purposes.
 */
public class SetBddLib<T> implements BddLib<Set<T>> {
    private final Set<T> universe;

    public SetBddLib(Set<T> universe) {
        this.universe = universe;
    }

    @Override
    public Set<T> constFalse() {
        return new HashSet<>();
    }

    @Override
    public Set<T> constTrue() {
        return universe;
    }

    @Override
    public boolean isConstFalse(Set<T> set) {
        return set.isEmpty();
    }

    @Override
    public boolean isConstTrue(Set<T> set) {
        return set.containsAll(universe);
    }

    @Override
    public Set<T> and(Set<T> left, Set<T> right) {
        final Set<T> result = new HashSet<>();

        // We iterate through the smaller set as a performance optimization

        final Set<T> smaller;
        final Set<T> larger;

        if (left.size() < right.size()) {
            smaller = left;
            larger = right;
        } else {
            smaller = right;
            larger = left;
        }

        for (T item : smaller) {
            if (larger.contains(item)) {
                result.add(item);
            }
        }

        return result;
    }

    @Override
    public Set<T> or(Set<T> left, Set<T> right) {
        final Set<T> result = new HashSet<>();
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    @Override
    public Set<T> not(Set<T> set) {
        final Set<T> result = new HashSet<>();

        for (T item : universe) {
            if (!set.contains(item)) {
                result.add(item);
            }
        }

        return result;
    }

    @Override
    public Set<T> ifThenElse(Set<T> cond, Set<T> thenClause, Set<T> elseClause) {
        return or(and(cond, thenClause), and(not(cond), elseClause));
    }

    @Override
    public Set<T> newVar() {
        throw new NotImplementedException();
    }

    @Override
    public String toString(Set<T> ts) {
        return ts.toString();
    }

    public String toString(SetBddLib<T> s) { return s.toString(); }
}