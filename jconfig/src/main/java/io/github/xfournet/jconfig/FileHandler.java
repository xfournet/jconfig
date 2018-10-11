package io.github.xfournet.jconfig;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface FileHandler {
    default boolean canHandle(Path file) {
        return false;
    }

    default Charset getCharset() {
        return UTF_8;
    }

    default void apply(Path file, List<String> instructions, Path destination) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    default Section diff(Path file, String fileName, @Nullable Path referenceFile) {
        throw new UnsupportedOperationException();
    }

    default void mergeFiles(Path source1, Path source2, Path destination) {
        throw new UnsupportedOperationException();
    }

    default void setEntries(Path file, Path destination, List<String> entries) {
        throw new UnsupportedOperationException();
    }

    default void removeEntries(Path file, Path destination, List<String> entries) {
        throw new UnsupportedOperationException();
    }

    default void normalize(Path file, Path destination) {
        throw new UnsupportedOperationException();
    }
}
