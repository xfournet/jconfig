package io.github.xfournet.jconfig;

import java.io.*;
import java.nio.file.Path;

/**
 * Represent a file.
 */
public interface FileEntry {
    /**
     * @return the file relative path
     */
    Path path();

    /**
     * @return a new {@code InputStream} for that file
     * @throws IOException in case of problem when {@code InputStream} is created
     */
    InputStream open() throws IOException;
}
