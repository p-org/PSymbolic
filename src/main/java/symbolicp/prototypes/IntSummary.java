package symbolicp.prototypes;

/**
 * Static helper methods implementing common operations on integer value summaries.
 * This class should never be instantiated.
 */
public class IntSummary {
    private IntSummary() {
    }

    public static <Bdd> ValueSummary<Bdd, Integer> add(
        ValueSummary<Bdd, Integer> left,
        ValueSummary<Bdd, Integer> right
    ) {
        return left.flatMap(leftValue ->
            right.flatMap(rightValue ->
                new ValueSummary<>(left.getBddLib(), leftValue + rightValue)
            )
        );
    }

    public static <Bdd> ValueSummary<Bdd, Integer> sub(
        ValueSummary<Bdd, Integer> left,
        ValueSummary<Bdd, Integer> right
    ) {
        return left.flatMap(leftValue ->
            right.flatMap(rightValue ->
                new ValueSummary<>(left.getBddLib(), leftValue - rightValue)
            )
        );
    }

    public static <Bdd> ValueSummary<Bdd, Integer> mul(
        ValueSummary<Bdd, Integer> left,
        ValueSummary<Bdd, Integer> right
    ) {
        return left.flatMap(leftValue ->
            right.flatMap(rightValue ->
                new ValueSummary<>(left.getBddLib(), leftValue * rightValue)
            )
        );
    }

    public static <Bdd> ValueSummary<Bdd, Integer> div(
        ValueSummary<Bdd, Integer> left,
        ValueSummary<Bdd, Integer> right
    ) {
        return left.flatMap(leftValue ->
            right.flatMap(rightValue ->
                new ValueSummary<>(left.getBddLib(), leftValue / rightValue)
            )
        );
    }
}
