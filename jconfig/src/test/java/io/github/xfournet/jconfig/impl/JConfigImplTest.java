package io.github.xfournet.jconfig.impl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import io.github.xfournet.jconfig.JConfig;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

public class JConfigImplTest {

    @DataProvider(name = "scenarios")
    public Object[][] providesConfScenarios() {
        return new Object[][]{ //
                {"scenario_1", "root_1",//
                        asList("conf/jvm.conf", "conf/log4j.properties", "conf/platform.properties", "var/data/default0.hash"), //
                        asList("conf/jvm.conf", "conf/log4j.properties", "conf/platform.properties", "var/data/default.hash")}, //
        };
    }

    @Test(dataProvider = "scenarios")
    public void testApplyScenario(String scenario, String sourcePrefix, List<String> sourceNames, List<String> resultNames) throws Exception {
        Path root = Paths.get("jconfig/" + scenario);

        Path iniFile = deploy(root, scenario, "jconfig.ini");

        Path testDir = root.resolve("test");
        for (String sourceName : sourceNames) {
            deploy(testDir, sourcePrefix, sourceName);
        }

        Path expectedDir = root.resolve("expected");
        for (String resultName : resultNames) {
            deploy(expectedDir, scenario + "/expected", resultName);
        }

        JConfig jConfig = JConfig.newJConfig();
        jConfig.apply(iniFile, testDir);

        Set<Path> validatedTestFiles = new HashSet<>();

        int expectedRootLen = expectedDir.toString().length() + 1;
        try (Stream<Path> expectedPaths = Files.walk(expectedDir)) {
            expectedPaths.filter(Files::isRegularFile).forEach(expectedFile -> {
                String relativeName = expectedFile.toString().substring(expectedRootLen);
                Path resultFile = testDir.resolve(relativeName);
                assertThat(resultFile).hasSameContentAs(expectedFile);
                validatedTestFiles.add(resultFile);
            });
        }

        try (Stream<Path> resultPaths = Files.walk(testDir)) {
            resultPaths //
                    .filter(Files::isRegularFile) //
                    .filter(resultFile -> !validatedTestFiles.contains(resultFile)) //
                    .forEach(resultFile -> fail("Unexpected file in result: " + resultFile));
        }
    }

    private Path deploy(Path root, String resourcePrefix, String name) throws IOException {
        Path output = root.resolve(name);
        try (InputStream in = JConfigImplTest.class.getResourceAsStream(resourcePrefix + "/" + name)) {
            Files.createDirectories(output.getParent());
            Files.copy(in, output, REPLACE_EXISTING);
        }
        return output;
    }
}
