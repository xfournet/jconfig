package io.github.xfournet.jconfig;

import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import io.github.xfournet.jconfig.impl.JConfigImpl;

public interface JConfig {

    static JConfig newJConfig() {
        return new JConfigImpl();
    }

    void apply(Path targetDir, Path iniFile);

    void diff(Path directory, Path referenceDir, Predicate<String> pathFilter, Path diffFile);

    void mergeFiles(Path source1, Path source2, Path destination);

    void setEntries(Path file, List<String> entries);

    void removeEntries(Path file, List<String> entries);

    void normalize(Path file);
}
