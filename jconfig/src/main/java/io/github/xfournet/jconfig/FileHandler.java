package io.github.xfournet.jconfig;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

public interface FileHandler {
    boolean canHandle(Path file);

    Charset getCharset();

    void mergeFiles(Path source1, Path source2, Path destination);

    void apply(Path file, List<String> instructions, Path destination);

    void setEntry(Path file, String entry, Path destination);

    void removeEntry(Path file, String entry, Path destination);

    void normalize(Path file, Path destination);
}
