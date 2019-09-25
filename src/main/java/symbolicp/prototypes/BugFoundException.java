package symbolicp.prototypes;

public class BugFoundException extends RuntimeException {
    final Object pathConstraint;

    public BugFoundException(String message, Object pathConstraint) {
        super(message);
        this.pathConstraint = pathConstraint;
    }
}
