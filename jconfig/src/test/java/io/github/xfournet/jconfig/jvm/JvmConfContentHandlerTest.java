package io.github.xfournet.jconfig.jvm;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.annotation.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import io.github.xfournet.jconfig.Diff;

import static io.github.xfournet.jconfig.Util.ensureCleanDirectory;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;

public class JvmConfContentHandlerTest {

    @DataProvider(name = "jvmDiff")
    public Object[][] providesJvmDiff() {
        return new Object[][]{//
                {Paths.get("jvm_diff1"), "jvm_1.txt", "jvm_2.txt", "jvm_diff_1_result.txt"}, //
                {Paths.get("jvm_diff2"), "jvm_2.txt", "jvm_1.txt", "jvm_diff_2_result.txt"}, //
                {Paths.get("jvm_diff3"), "jvm_1.txt", "jvm_1.txt", null}, //
                {Paths.get("jvm_diff4"), "jvm_1.txt", null, "jvm_1.txt"}, //
                {Paths.get("jvm_diff5"), "jvm_2.txt", null, "jvm_2.txt"}, //
        };
    }

    @Test(dataProvider = "jvmDiff")
    public void testJvmDiff(Path root, String source1Name, @Nullable String source2Name, @Nullable String expectedResultName) throws Exception {
        ensureCleanDirectory(root);
        Path expectedFile = root.resolve("expected");
        Path resultFile = root.resolve("result");

        if (expectedResultName != null) {
            try (InputStream expectedResultInput = JvmConfContentHandlerTest.class.getResourceAsStream(expectedResultName)) {
                Files.copy(expectedResultInput, expectedFile);
            }
        }

        Diff diff;
        try (InputStream source1 = JvmConfContentHandlerTest.class.getResourceAsStream(source1Name);
             InputStream source2 = source2Name != null ? JvmConfContentHandlerTest.class.getResourceAsStream(source2Name) : null) {

            diff = new JvmConfContentHandler().diff(source1, source2);
        }

        if (expectedResultName != null) {
            assertThat(diff).isNotNull();
            assertThat(diff.isOverwrite()).isEqualTo(source2Name == null);
            assertThat(diff.getEncoding()).isNull();
            Files.write(resultFile, diff.getLines());
            assertThat(resultFile).hasSameContentAs(expectedFile);
        } else {
            assertThat(diff).isNull();
        }
    }

    @DataProvider(name = "jvmApply")
    public Object[][] providesJvmApply() {
        return new Object[][]{//
                {Paths.get("jvm_apply1"), "jvm_1.txt", "jvm_diff_1.txt", false, "jvm_apply_1_result.txt"}, //
                {Paths.get("jvm_apply2"), "jvm_2.txt", "jvm_diff_1.txt", false, "jvm_apply_2_result.txt"}, //
                {Paths.get("jvm_apply3"), null, "jvm_diff_1.txt", false, "jvm_apply_3_result.txt"}, //
                {Paths.get("jvm_apply4"), null, "jvm_1.txt", true, "jvm_1.txt"}, //
                {Paths.get("jvm_apply5"), null, "jvm_2.txt", true, "jvm_2.txt"}, //
        };
    }

    @Test(dataProvider = "jvmApply")
    public void testJvmApply(Path root, @Nullable String sourceName, String diffName, boolean overwrite, String expectedResultName) throws Exception {
        ensureCleanDirectory(root);
        Path diffFile = root.resolve("diff");
        Path expectedFile = root.resolve("expected");
        Path resultFile = root.resolve("result");

        try (InputStream source = sourceName != null ? JvmConfContentHandlerTest.class.getResourceAsStream(sourceName) : null;
             InputStream diffSource = JvmConfContentHandlerTest.class.getResourceAsStream(diffName);
             InputStream expectedResultInput = JvmConfContentHandlerTest.class.getResourceAsStream(expectedResultName);
             OutputStream result = Files.newOutputStream(resultFile)) {
            Files.copy(expectedResultInput, expectedFile);
            Files.copy(diffSource, diffFile);

            List<String> lines = Files.readAllLines(diffFile);
            Diff diff = new Diff(overwrite, null, lines);

            new JvmConfContentHandler().apply(source, result, diff);
        }

        assertThat(resultFile).hasSameContentAs(expectedFile);
    }

    @DataProvider(name = "jvmMerge")
    public Object[][] providesJvmMerge() {
        return new Object[][]{ //
                {Paths.get("jvm_merge1"), "jvm_2.txt", "jvm_1.txt", "jvm_merge_1_result.txt"}, //
        };
    }

    @Test(dataProvider = "jvmMerge")
    public void testJvmMerge(Path root, String source1Name, String source2Name, String expectedResultName) throws Exception {
        ensureCleanDirectory(root);
        Path expectedFile = root.resolve("expected");
        Path resultFile = root.resolve("result");

        try (InputStream source1 = JvmConfContentHandlerTest.class.getResourceAsStream(source1Name);
             InputStream source2 = JvmConfContentHandlerTest.class.getResourceAsStream(source2Name);
             InputStream expectedResultInput = JvmConfContentHandlerTest.class.getResourceAsStream(expectedResultName);
             OutputStream result = Files.newOutputStream(resultFile)) {

            Files.copy(expectedResultInput, expectedFile);
            new JvmConfContentHandler().merge(source1, source2, result);
        }

        assertThat(resultFile).hasSameContentAs(expectedFile);
    }

    @DataProvider(name = "jvmSetEntry")
    public Object[][] providesJvmSetEntry() {
        return new Object[][]{ //
                {Paths.get("jvm_setEntry1"), "jvm_1.txt", "-Xmx1G", "jvm_set_1_result.txt"}, //
        };
    }

    @Test(dataProvider = "jvmSetEntry")
    public void testJvmSet(Path root, String sourceName, String entry, String expectedResultName) throws Exception {
        ensureCleanDirectory(root);
        Path expectedFile = root.resolve("expected");
        Path resultFile = root.resolve("result");

        try (InputStream source = JvmConfContentHandlerTest.class.getResourceAsStream(sourceName);
             InputStream expectedResultInput = JvmConfContentHandlerTest.class.getResourceAsStream(expectedResultName);
             OutputStream result = Files.newOutputStream(resultFile)) {

            Files.copy(expectedResultInput, expectedFile);
            new JvmConfContentHandler().setEntries(source, result, singletonList(entry));
        }

        assertThat(resultFile).hasSameContentAs(expectedFile);
    }

    @DataProvider(name = "jvmRemoveEntry")
    public Object[][] providesJvmRemoveEntry() {
        return new Object[][]{ //
                {Paths.get("jvm_removeEntry1"), "jvm_1.txt", "-Xmx0", "jvm_remove_1_result.txt"}, //
        };
    }

    @Test(dataProvider = "jvmRemoveEntry")
    public void testJvmRemove(Path root, String sourceName, String entry, String expectedResultName) throws Exception {
        ensureCleanDirectory(root);
        Path expectedFile = root.resolve("expected");
        Path resultFile = root.resolve("result");

        try (InputStream source = JvmConfContentHandlerTest.class.getResourceAsStream(sourceName);
             InputStream expectedResultInput = JvmConfContentHandlerTest.class.getResourceAsStream(expectedResultName);
             OutputStream result = Files.newOutputStream(resultFile)) {

            Files.copy(expectedResultInput, expectedFile);
            new JvmConfContentHandler().removeEntries(source, result, singletonList(entry));
        }

        assertThat(resultFile).hasSameContentAs(expectedFile);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Unrecognized jvm.conf option: .*")
    public void testInvalidJvmConf() throws Exception {
        try (InputStream source = JvmConfContentHandlerTest.class.getResourceAsStream("jvm_invalid.txt")) {
            new JvmConfContentHandler().setEntries(source, new ByteArrayOutputStream(), Collections.emptyList());
        }
    }
}
