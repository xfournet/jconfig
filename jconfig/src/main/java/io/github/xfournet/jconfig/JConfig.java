package io.github.xfournet.jconfig;

import java.nio.file.Path;
import io.github.xfournet.jconfig.impl.JConfigImpl;

public interface JConfig {

    static JConfig newJConfig() {
        return new JConfigImpl();
    }

    void mergeFiles(Path source1, Path source2, Path destination);

    void setEntry(Path file, String entry);

    void removeEntry(Path file, String entry);

    void normalize(Path file);

    void apply(Path iniFile, Path targetDir);
}
