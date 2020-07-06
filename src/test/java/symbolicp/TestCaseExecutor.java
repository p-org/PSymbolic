package symbolicp;
import symbolicp.runtime.CompilerLogger;
import symbolicp.runtime.RuntimeLogger;
import symbolicp.run.EntryPoint;
import symbolicp.run.Program;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;


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
     * @param testCasePaths paths to test case; only accepts list of p files
     * @return 0 = successful, 1 = compile error, 2 = dynamic error
     */
    static int runTestCase(List<String> testCasePaths) {
        // Invoke the P compiler to compile the test Case
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
        String compilerDirectory = "../Bld/Drops/Release/Binaries/Pc.dll";

        String prefix = "../Tst/";
        assert testCasePaths.stream().allMatch(p -> p.contains(prefix));
        List<String> testCaseRelPaths = testCasePaths.stream().map(p -> p.substring(p.indexOf(prefix) + prefix.length()))
                                        .collect(Collectors.toList());
        testCasePaths.stream().map(p -> p.substring(p.indexOf(prefix) + prefix.length())).forEach(System.out::println);
        //System.out.println(testCasePaths.toString());
        String testCaseRelDir = sanitizeRelDir(Paths.get(testCaseRelPaths.get(0)).getParent().toString());
        String outputDirectory = "src/test/java/symbolicp/testCase/" + testCaseRelDir;
        String outputPackage = packageNameFromRelDir(testCaseRelDir);

        String testCasePathsString = String.join(" ", testCasePaths);
        Process process;
        try {
            System.out.println(String.format("dotnet %s %s -generate:Symbolic -outputDir:%s\n"
                                                   , compilerDirectory, testCasePathsString, outputDirectory));
            if (isWindows) {
                process = Runtime.getRuntime()
                        .exec(String.format("dotnet %s %s -generate:Symbolic -outputDir:%s"
                                , compilerDirectory, testCasePathsString, outputDirectory));
            } else {
                process = Runtime.getRuntime()
                        .exec(String.format("dotnet %s %s -generate:Symbolic -outputDir:%s " , compilerDirectory, testCasePathsString, outputDirectory));
            }

            if (PRINT_STATIC_ERRORS) {
                StreamGobbler streamGobbler = new StreamGobbler(process.getErrorStream(), System.out::println);
                Executors.newSingleThreadExecutor().submit(streamGobbler);
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                CompilerLogger.log("Compilation failure.");
                return 1;
            }
        }
        catch (IOException | InterruptedException e) {
            CompilerLogger.log("Compilation failure.");
            e.printStackTrace();
        }

        // Next, try to dynamically load and compile this file
        String[] path_split = Utils.splitPath(testCasePaths.get(0));
        String class_name = path_split[path_split.length-1].split("\\.")[0].toLowerCase();
        String outputPath = outputDirectory + File.separator + class_name + ".java";

        // Program to run
        Program p = null;

        // Try to compile the file
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, outputPath);



        // Load and instantiate compiled class
        try {
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File(outputDirectory).toURI().toURL()});
            Class<?> cls = Class.forName(class_name, true, classLoader);
            Object instance = cls.getDeclaredConstructor().newInstance();
            p = (Program) instance;
        } catch (InstantiationException | MalformedURLException | IllegalAccessException | ClassNotFoundException |
                NoSuchMethodException | InvocationTargetException e) {
            CompilerLogger.log("Compilation failure.");
            e.printStackTrace();
            return 1;
        }
        try {
            EntryPoint.run(p, 200, 50);
        } catch (Exception | AssertionError e) {
            e.printStackTrace();
            return 2;
        } finally {
            new File(outputDirectory + File.separator + class_name + ".class").delete();
        }

        return 0;
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
