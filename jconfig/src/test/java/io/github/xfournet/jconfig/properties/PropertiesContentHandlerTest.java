package io.github.xfournet.jconfig.properties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.github.xfournet.jconfig.Util.ensureCleanDirectory;
import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesContentHandlerTest {

    @DataProvider(name = "propertiesMerge")
    public Object[][] providesPropertiesMerge() {
        return new Object[][]{{ //
                Paths.get("properties_merge1"), "file_2.properties", "file_1.properties", "merge_1_result.properties"}, //
        };
    }

    @Test(dataProvider = "propertiesMerge")
    public void testPropertiesMerge(Path root, String source1Name, String source2Name, String expectedResultName) throws Exception {
        ensureCleanDirectory(root);
        Path expectedFile = root.resolve("expected");
        Path resultFile = root.resolve("result");

        try (InputStream source1 = PropertiesContentHandlerTest.class.getResourceAsStream(source1Name);
             InputStream source2 = PropertiesContentHandlerTest.class.getResourceAsStream(source2Name);
             InputStream expectedResultInput = PropertiesContentHandlerTest.class.getResourceAsStream(expectedResultName);
             OutputStream result = Files.newOutputStream(resultFile)) {

            Files.copy(expectedResultInput, expectedFile);

            new PropertiesContentHandler().merge(source1, source2, result);
        }

        assertThat(resultFile).hasSameContentAs(expectedFile);
    }

    @DataProvider(name = "propertiesFiltering")
    public Object[][] providesPropertiesFiltering() {
        Map<String, String> vars = new HashMap<>();
        vars.put("var1", "999");
        vars.put("var2", "80%");

        return new Object[][]{{ //
                Paths.get("properties_filter1"), "file_2.properties", vars, "filter_1_result.properties"}, //
        };
    }

    @Test(dataProvider = "propertiesFiltering")
    public void testPropertiesFiltering(Path root, String sourceName, Map<String, String> vars, String expectedResultName) throws Exception {
        ensureCleanDirectory(root);
        Path expectedFile = root.resolve("expected");
        Path resultFile = root.resolve("result");

        try (InputStream source = PropertiesContentHandlerTest.class.getResourceAsStream(sourceName);
             InputStream expectedResultInput = PropertiesContentHandlerTest.class.getResourceAsStream(expectedResultName);
             OutputStream result = Files.newOutputStream(resultFile)) {

            Files.copy(expectedResultInput, expectedFile);

            new PropertiesContentHandler().filter(source, result, vars::get);
        }

        assertThat(resultFile).hasSameContentAs(expectedFile);
    }
}
