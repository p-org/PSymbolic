package symbolicp.bdd;

import jsylvan.JSylvan;
import symbolicp.util.NotImplementedException;

import java.io.IOException;


public class JSylvanBdd implements BddLib<Long> {

    private static boolean initialized = false;

    private int counter = 0;

    public JSylvanBdd() {
        if (!initialized) {
            try {
                JSylvan.init(2, 400L*1024*1024, 1, 4, 1);
                System.out.println("Initialized Lace and Sylvan.");
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
            JSylvan.disableGC();
            JSylvan.enableGC();
            initialized = true;
        }
    }

    @Override
    public Long constFalse() {
        return JSylvan.getFalse();
    }

    @Override
    public Long constTrue() {
        return JSylvan.getTrue();
    }

    @Override
    public boolean isConstFalse(Long aLong) {
        return aLong == JSylvan.getFalse();
    }

    @Override
    public boolean isConstTrue(Long aLong) {
        return aLong == JSylvan.getTrue();
    }

    @Override
    public Long and(Long left, Long right) {
        return JSylvan.ref(JSylvan.makeAnd(left, right));
    }

    @Override
    public Long or(Long left, Long right) {
        return JSylvan.ref(JSylvan.makeOr(left, right));
    }

    @Override
    public Long not(Long aLong) {
        return JSylvan.ref(JSylvan.makeNot(aLong));
    }

    @Override
    public Long ifThenElse(Long cond, Long thenClause, Long elseClause) {
        return JSylvan.ref(JSylvan.makeIte(cond, thenClause, elseClause));
    }

    @Override
    public Long newVar() {
        counter ++;
        return JSylvan.ref(JSylvan.makeVar(counter));
    }

    @Override
    public String toString(Long a) {
        throw new NotImplementedException();
    }

    @Override
    public Long fromString(String s) {
        throw new NotImplementedException();
    }
}
