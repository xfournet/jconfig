package io.github.xfournet.jconfig;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.*;

public final class Util {

    public static void ensureCleanDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> pathStream = Files.walk(dir)) {
                for (Path path : pathStream.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(dir);
    }

    private Util() {
    }
}
