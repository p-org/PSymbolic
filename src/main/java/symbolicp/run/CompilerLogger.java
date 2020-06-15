package symbolicp.run;

import java.util.Arrays;
import java.util.logging.Logger;

public class CompilerLogger {
    private final static Logger log = Logger.getLogger("Compiler");

    public static void log(Object ... message) {
       log.info("<Compiler> " + String.join(", ", Arrays.toString(message)));
    }
}