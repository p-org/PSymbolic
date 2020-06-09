package symbolicp;
import org.junit.jupiter.api.Test;
import symbolicp.runtime.Machine;
import symbolicp.runtime.Scheduler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class TestExecutorDemo {
    /***
        This automated pipeline would first compile the java file into the testCase folder in P, then dynamically
        compile this file and perform symbolic execution.
     ***/
    @Test
    public void testOneWaySend() throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        // Search compiled test file for
        Class<?> mainMachineClass = Class.forName("symbolicp.testCase.accumulatortest$machine_Main");
        Class<?> wrapper_class =  Class.forName("symbolicp.testCase.accumulatortest");
        Object wrapper = wrapper_class.getConstructor().newInstance();

        Constructor constructor = mainMachineClass.getConstructor(int.class);
        Machine main = (Machine) constructor.newInstance(0);

        Scheduler scheduler = new Scheduler(Utils.getMachines(wrapper_class, wrapper));
        wrapper_class.getField("scheduler").set(wrapper, scheduler);
        scheduler.disableLogging();
        scheduler.startWith(main);

        int max_depth = 100;
        for (int i = 0; i < max_depth; ++i) {
            if (scheduler.step()) return;
        }

        assert false; // Non termination
    }
}
