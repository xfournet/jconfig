package io.github.xfournet.jconfig.properties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesHandlerTest {

    @DataProvider(name = "propertiesMerge")
    public Object[][] providesPropertiesMerge() {
        return new Object[][]{{ //
                Paths.get("properties_merge1"), "file_1.properties", "file_2.properties", "merge_1_result.properties"}, //
        };
    }

    @Test(dataProvider = "propertiesMerge")
    public void testPropertiesMerge(Path root, String source1Name, String source2Name, String expectedResultName) throws Exception {
        Path source1 = resourceToPath(root, source1Name);
        Path source2 = resourceToPath(root, source2Name);
        Path expectedResult = resourceToPath(root, expectedResultName);

        Path result = Paths.get(expectedResult.toString() + ".test");

        PropertiesHandler propertiesHandler = new PropertiesHandler();
        assertThat(propertiesHandler.canHandle(source1)).isTrue();
        assertThat(propertiesHandler.canHandle(source2)).isTrue();
        propertiesHandler.mergeFiles(source1, source2, result);

        assertThat(result).hasSameContentAs(expectedResult);
    }

    private Path resourceToPath(Path root, String name) throws IOException {
        Path output = root.resolve(name);
        try (InputStream in = PropertiesHandlerTest.class.getResourceAsStream(name)) {
            Files.createDirectories(output.getParent());
            Files.copy(in, output, REPLACE_EXISTING);
        }
        return output;
    }
}
