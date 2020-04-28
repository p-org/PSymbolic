package symbolicp;
import org.joor.Reflect;
import java.io.*;
import symbolicp.runtime.BaseMachine;
import symbolicp.runtime.MachineTag;
import symbolicp.runtime.Scheduler;
import symbolicp.vs.EventVS;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class TestCaseExecutor {
    private final static boolean PRINT_STATIC_ERRORS = true;
    /**
     * @param testCasePath path to test case; only accepts p file
     * @return 0 = successful, 1 = compile error, 2 = dynamic error
     */
    static int runTestCase(String testCasePath) {

        // Invoke the P compiler to compile the test Case
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
        String compilerDirectory = "../Bld/Drops/Release/Binaries/Pc.dll";
        String outputDirectory = "src/test/java/symbolicp/testCase";

        Process process;
        try {
            if (isWindows) {
                process = Runtime.getRuntime()
                        .exec(String.format("dotnet %s %s -generate:Symbolic -outputDir:%s"
                                , compilerDirectory, testCasePath, outputDirectory));
            } else {
                process = Runtime.getRuntime()
                        .exec(String.format("dotnet %s %s -generate:Symbolic -outputDir:%s " , compilerDirectory, testCasePath, outputDirectory));
            }

            if (PRINT_STATIC_ERRORS) {
                StreamGobbler streamGobbler = new StreamGobbler(process.getErrorStream(), System.out::println);
                Executors.newSingleThreadExecutor().submit(streamGobbler);
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) return 1;
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // Next, try to dynamically load and compile this file
        String[] path_split = Utils.splitPath(testCasePath);
        String class_name = path_split[path_split.length-1].split("\\.")[0].toLowerCase();
        String outputPath = outputDirectory + File.separator + class_name + ".java";
        String file_content = readLineByLineJava8(outputPath);

        Reflect.compile(class_name, file_content);
        try {
            Class<?> wrapper_class = Class.forName("symbolicp.testCase." + class_name);
            Class<?> mainMachineClass = Class.forName("symbolicp.testCase." + class_name + "$machine_Main");

            Object wrapper = wrapper_class.getConstructor().newInstance();

            MachineTag machineTag_Main = (MachineTag) wrapper_class.getField("machineTag_Main").get(wrapper);
            EventVS.Ops eventOps =  (EventVS.Ops) wrapper_class.getField("eventOps").get(wrapper);

            Constructor constructor = mainMachineClass.getConstructor(int.class);
            BaseMachine main = (BaseMachine) constructor.newInstance(0);

            Scheduler scheduler = new Scheduler(eventOps, Utils.getMachineTags(wrapper_class, wrapper));
            wrapper_class.getField("scheduler").set(wrapper, scheduler);
            scheduler.disableLogging();
            scheduler.startWith(machineTag_Main, main);

            int max_depth = 100;
            for (int i = 0; i < max_depth; ++i) {
                if (scheduler.step()) return 0;
            }
        }
        catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                IllegalAccessException | NoSuchFieldException | NoSuchMethodException e) {
            // Dynamic compilation exceptions are considered as Static Errors
            e.printStackTrace();
            return 1;
        }
        catch (Exception e) {
            // Other exceptions are considered as Dynamic Errors
            e.printStackTrace();
            return 2;
        }
        return 2;
    }

    //Java 8 - Read file line by line - Files.lines(Path path, Charset cs)
    private static String readLineByLineJava8(String filePath)
    {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }

}
