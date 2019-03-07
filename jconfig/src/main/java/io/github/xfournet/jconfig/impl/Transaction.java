package io.github.xfournet.jconfig.impl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;

final class Transaction implements AutoCloseable {
    private final String m_tmpFileSuffix = "." + System.currentTimeMillis() + ".tmp";
    private final List<FileOperation> m_commitOperations = new ArrayList<>();
    private final List<FileOperation> m_rollbackOperations = new ArrayList<>();

    /**
     * Ensures that the given directory exists. If it doesn't exist yet it's created and will be removed upon rollback
     *
     * @param path Directory to create
     */
    void ensureDirectory(Path path) {
        if (!Files.exists(path)) {
            Path parent = path.getParent();
            if (parent != null) {
                ensureDirectory(parent);
            }

            m_rollbackOperations.add(() -> Files.deleteIfExists(path));
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot create the directory", e);
            }
        }
    }

    /**
     * Provides a temporary file for writing the content of the given file. The reference file will be replaced during commit, the temporary file is always
     * removed afterwards.
     *
     * @param file File to update
     * @return Temporary file for writing in it
     */
    Path updateFile(Path file) {
        Path tmpFile = Paths.get(file.toString() + m_tmpFileSuffix);
        m_rollbackOperations.add(() -> Files.deleteIfExists(tmpFile));

        Path parent = tmpFile.getParent();
        if (parent != null) {
            ensureDirectory(parent);
        }
        m_commitOperations.add(() -> Files.move(tmpFile, file, REPLACE_EXISTING, ATOMIC_MOVE));
        return tmpFile;
    }

    /**
     * Deletes a file. Will only be deleted during commit, rollback does nothing.
     *
     * @param file File to delete
     */
    void deleteFile(Path file) {
        m_commitOperations.add(() -> Files.deleteIfExists(file));
    }

    /**
     * Commits the transaction. All file modifications are applied.
     */
    void commit() {
        try {
            m_commitOperations.forEach(FileOperation::safeRun);
        } finally {
            m_commitOperations.clear();
            m_rollbackOperations.clear();
        }
    }

    /**
     * Closes the transaction, performs a rollback if it's not committed (ie interrupted by an exception)
     */
    @Override
    public void close() {
        Collections.reverse(m_rollbackOperations);
        try {
            m_rollbackOperations.forEach(FileOperation::safeRun);
        } finally {
            m_commitOperations.clear();
            m_rollbackOperations.clear();
        }
    }

    @FunctionalInterface
    private interface FileOperation {
        void run() throws IOException;

        default void safeRun() {
            try {
                run();
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot run I/O operation", e);
            }
        }
    }
}
