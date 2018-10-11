package io.github.xfournet.jconfig;

import java.nio.file.Path;
import java.util.*;
import io.github.xfournet.jconfig.impl.JConfigImpl;

public interface JConfig {

    static JConfig newJConfig() {
        return new JConfigImpl();
    }

    void apply(Path iniFile, Path targetDir);

    void mergeFiles(Path source1, Path source2, Path destination);

    void setEntries(Path file, List<String> entries);

    void removeEntries(Path file, List<String> entries);

    void normalize(Path file);
}
