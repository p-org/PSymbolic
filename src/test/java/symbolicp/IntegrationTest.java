package symbolicp;
import org.junit.Test;
import symbolicp.runtime.BaseMachine;
import symbolicp.runtime.MachineTag;
import symbolicp.runtime.Scheduler;
import symbolicp.vs.EventVS;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class IntegrationTest {
    /***
        This automated pipeline would first compile the java file into the testCase folder in P, then dynamically
        compile this file and perform symbolic execution.
     ***/
    @Test
    public void testOneWaySend() throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        // Search compiled test file for
        Class<?> mainMachineClass = Class.forName("symbolicp.testCase.accumulatorTest$machine_Main");
        Class<?> wrapper_class =  Class.forName("symbolicp.testCase.accumulatorTest");
        Object wrapper = wrapper_class.getConstructor().newInstance();

        MachineTag machineTag_Main = (MachineTag) wrapper_class.getField("machineTag_Main").get(wrapper);
        EventVS.Ops eventOps =  (EventVS.Ops) wrapper_class.getField("eventOps").get(wrapper);

        Constructor constructor = mainMachineClass.getConstructor(int.class);
        BaseMachine main = (BaseMachine) constructor.newInstance(0);

        Scheduler scheduler = new Scheduler(eventOps, Utils.getMachineTags(wrapper_class, wrapper));
        wrapper_class.getField("scheduler").set(wrapper, scheduler);
        //scheduler.disableLogging();
        scheduler.startWith(machineTag_Main, main);

        int max_depth = 100;
        for (int i = 0; i < max_depth; ++i)
            if (scheduler.step()) break;
    }
}
