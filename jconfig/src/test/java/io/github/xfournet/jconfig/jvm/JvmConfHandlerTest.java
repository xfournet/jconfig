package io.github.xfournet.jconfig.jvm;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;

public class JvmConfHandlerTest {

    @DataProvider(name = "jvmMerge")
    public Object[][] providesJvmMerge() {
        return new Object[][]{{ //
                Paths.get("jvm_merge1"), "jvm_1.txt", "jvm_2.txt", "jvm_merge_1_result.txt"}, //
        };
    }

    @Test(dataProvider = "jvmMerge")
    public void testJvmMerge(Path root, String source1Name, String source2Name, String expectedResultName) throws Exception {
        Path source1 = resourceToPath(root, source1Name);
        Path source2 = resourceToPath(root, source2Name);
        Path expectedResult = resourceToPath(root, expectedResultName);

        Path result = Paths.get(expectedResult.toString() + ".test");

        new JvmConfHandler().mergeFiles(source1, source2, result);

        assertThat(result).hasSameContentAs(expectedResult);
    }

    @DataProvider(name = "jvmSetEntry")
    public Object[][] providesJvmSetEntry() {
        return new Object[][]{{ //
                Paths.get("jvm_setEntry1"), "jvm_1.txt", "-Xmx1G", "jvm_set_1_result.txt"}, //
        };
    }

    @Test(dataProvider = "jvmSetEntry")
    public void testJvmSet(Path root, String sourceName, String entry, String expectedResultName) throws Exception {
        Path source = resourceToPath(root, sourceName);
        Path expectedResult = resourceToPath(root, expectedResultName);

        new JvmConfHandler().setEntries(source, source, singletonList(entry));

        assertThat(source).hasSameContentAs(expectedResult);
    }

    @DataProvider(name = "jvmRemoveEntry")
    public Object[][] providesJvmRemoveEntry() {
        return new Object[][]{{ //
                Paths.get("jvm_removeEntry1"), "jvm_1.txt", "-Xmx0", "jvm_remove_1_result.txt"}, //
        };
    }

    @Test(dataProvider = "jvmRemoveEntry")
    public void testJvmRemove(Path root, String sourceName, String entry, String expectedResultName) throws Exception {
        Path source = resourceToPath(root, sourceName);
        Path expectedResult = resourceToPath(root, expectedResultName);

        new JvmConfHandler().removeEntries(source, source, singletonList(entry));

        assertThat(source).hasSameContentAs(expectedResult);
    }

    @DataProvider(name = "jvmNormalize")
    public Object[][] providesJvmNormalize() {
        return new Object[][]{{ //
                Paths.get("jvm_normalize1"), "jvm_1.txt", "jvm_normalize_1_result.txt"}, //
        };
    }

    @Test(dataProvider = "jvmNormalize")
    public void testJvmNormalize(Path root, String sourceName, String expectedResultName) throws Exception {
        Path source = resourceToPath(root, sourceName);
        Path expectedResult = resourceToPath(root, expectedResultName);

        new JvmConfHandler().normalize(source, source);

        assertThat(source).hasSameContentAs(expectedResult);
    }

    private Path resourceToPath(Path root, String name) throws IOException {
        Path output = root.resolve(name);
        try (InputStream in = JvmConfHandlerTest.class.getResourceAsStream(name)) {
            Files.createDirectories(output.getParent());
            Files.copy(in, output, REPLACE_EXISTING);
        }
        return output;
    }
}
