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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class TestCaseExecutor {
    // This can cause collisions if you have two very similar directory names, like 'Foo_Bar' and 'Foo-Bar', but that
    // should be (?) acceptable for our purposes.
    private static String sanitizeRelDir(String relDir) {
        return relDir.replace(' ', '_').replace('-', '_');
    }

    private static String packageNameFromRelDir(String relDir) {
        assert relDir.equals(sanitizeRelDir(relDir));
        assert !relDir.contains("//");
        assert !relDir.startsWith("/");
        assert !relDir.endsWith("/");
        return "symbolicp.testCase." + relDir.replace('/', '.');
    }

    // We prepend the package directly to the file on disk, rather than to the file contents we read into memory, to
    // permit manual testing of the generated files from IntelliJ.
    private static String prependPackageDeclarationAndRead(String packageName, String filePath) {
        try {
            String fileContents = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            fileContents = "package " + packageName + ";\n" + fileContents;

            FileWriter writer = new FileWriter(filePath);
            writer.append(fileContents);
            writer.close();

            return fileContents;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

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

        String prefix = "../Tst/";
        assert testCasePath.startsWith(prefix);
        String testCaseRelPath = testCasePath.substring(prefix.length());
        String testCaseRelDir = sanitizeRelDir(Paths.get(testCaseRelPath).getParent().toString());
        String outputDirectory = "src/test/java/symbolicp/testCase/" + testCaseRelDir;
        String outputPackage = packageNameFromRelDir(testCaseRelDir);

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
        String class_name = path_split[path_split.length-1].split("\\.")[0];
        String outputPath = outputDirectory + File.separator + class_name + ".java";
        String fileContent = prependPackageDeclarationAndRead(outputPackage, outputPath);
        try{
            Reflect.compile(class_name, fileContent);
        }
        catch (Exception e) {
            // Dynamic compilation exceptions are considered as Static Errors
            e.printStackTrace();
            return 1;
        }
        try {
            Class<?> wrapper_class = Class.forName(outputPackage + "." + class_name);
            Class<?> mainMachineClass = Class.forName(outputPackage + "." + class_name + "$machine_Main");

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
            return 0;
        }
        catch (Exception | AssertionError e) {
            // Runtime exceptions are considered as Dynamic Errors
            e.printStackTrace();
            return 2;
        }
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
