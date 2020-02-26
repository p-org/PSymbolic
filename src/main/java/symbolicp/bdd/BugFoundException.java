package symbolicp.bdd;

public class BugFoundException extends RuntimeException {
    final Object pathConstraint;

    public BugFoundException(String message, Bdd pathConstraint) {
        super(message);
        this.pathConstraint = pathConstraint;
    }
}
