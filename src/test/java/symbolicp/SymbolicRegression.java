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

/**
 * Runner for Symbolic P Regressions.
 * Pre-requisites:
 *  Run from P repository as a submodule
 *  Build the symbolic compiler to ../Bld/Drops/Release/Binaries/Pc.dll
 *  Place test cases as source P files at ../Tst/SymbolicRegressionTests/
 */
public class SymbolicRegression {

    @TestFactory
    Collection<DynamicTest> loadTests() {

        Collection<DynamicTest> dynamicTests = new ArrayList<>();

        // First, fetch all files that are within the Regression Test folder.
        List<String> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Paths.get("../Tst/RegressionTests/"))) {
             result = walk.map(Path::toString)
                    .filter(f -> f.endsWith(".p")).collect(Collectors.toList());

            result.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String testCasePath : result) {
            Executable exec = null;
            if (testCasePath.contains("Correct")) {
                exec = () -> assertEquals(0, TestCaseExecutor.runTestCase(testCasePath));
            }
            else if (testCasePath.contains("DynamicError")) {
                exec = () -> assertEquals(2, TestCaseExecutor.runTestCase(testCasePath));
            }
            else if (testCasePath.contains("StaticError")) {
                exec = () -> assertEquals(1, TestCaseExecutor.runTestCase(testCasePath));
            }
            else {
                continue;
            }

            DynamicTest dynamicTest = DynamicTest.dynamicTest(testCasePath, exec);
            dynamicTests.add(dynamicTest);
        }

        return dynamicTests;
    }

}
