package symbolicp.runtime;

import java.util.Arrays;
import java.util.logging.Logger;

public class RuntimeLogger {
    private final static Logger log = Logger.getLogger("Runtime");

    public static void log(Object ... message) {
       log.info("<PrintLog> " + String.join(", ", Arrays.toString(message)));
    }
}