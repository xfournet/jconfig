package io.github.xfournet.jconfig;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

public interface FileHandler {
    boolean canHandle(Path file);

    Charset getCharset();

    void apply(Path file, List<String> instructions, Path destination);

    void mergeFiles(Path source1, Path source2, Path destination);

    void setEntries(Path file, Path destination, List<String> entries);

    void removeEntries(Path file, Path destination, List<String> entries);

    void normalize(Path file, Path destination);
}
