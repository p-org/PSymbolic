package symbolicp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import org.junit.jupiter.api.function.Executable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestCaseLoader {

    @TestFactory
    Collection<DynamicTest> loadTests() {

        Collection<DynamicTest> dynamicTests = new ArrayList<>();

        // First, fetch all files that are within the Regression Test folder.
        List<String> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Paths.get("../Tst/SymbolicRegressionTests/"))) {
             result = walk.map(Path::toString)
                    .filter(f -> f.endsWith(".p")).collect(Collectors.toList());

            result.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String testCasePath : result) {
            Executable exec = null;
            if (testCasePath.contains("Correct")) {
                exec = () -> assertEquals(TestCaseExecutor.runTestCase(testCasePath), 0);
            }
            else if (testCasePath.contains("DynamicError")) {
                exec = () -> assertEquals(TestCaseExecutor.runTestCase(testCasePath), 2);
            }
            else if (testCasePath.contains("StaticError")) {
                exec = () -> assertEquals(TestCaseExecutor.runTestCase(testCasePath), 1);
            }

            DynamicTest dynamicTest = DynamicTest.dynamicTest(testCasePath, exec);
            dynamicTests.add(dynamicTest);
        }

        return dynamicTests;
    }

}
