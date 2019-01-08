package io.github.xfournet.jconfig;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.*;

/**
 * Entry point to use JConfig.
 */
public interface JConfig {
    /**
     * @return the target directory used by others methods
     */
    Path targetDir();

    /**
     * Apply a diff file to {@link #targetDir()}.<br>
     * {@code apply(targetDir, diffFile) => targetDir'}
     *
     * @param diffFile the diff file to be applied
     */
    void apply(Path diffFile);

    /**
     * Generate a diff file by comparing the {@link #targetDir()} with a reference one. Applying generated diff file to reference directory should give the same than the target directory.<br>
     * {@code diff(targetDir, referenceDir) => diffFile}<br>
     * {@code apply(referenceDir, diffFile) => targetDir}
     *
     * @param referenceDir the reference directory
     * @param diffFile the diff file result
     */
    void diff(Path referenceDir, Path diffFile);

    /**
     * Merge a directory or a ZIP file to the {@link #targetDir()}.<br>
     * {@code merge(targetDir, source) => targetDir'}
     *
     * @param source the source to be applied to the target. This can be either a directory or a ZIP file.
     */
    void merge(Path source);

    /**
     * Merge contents to the {@link #targetDir()}.<br>
     * {@code merge(targetDir, contents) => targetDir'}
     *
     * @param sourceFileEntries a {@code Stream} of {@link FileEntry} to be merged to the directory
     */
    void merge(Stream<? extends FileEntry> sourceFileEntries);

    /**
     * Merge two files.<br>
     * {@code merge(targetDir/targetFile, sourceFile) => targetDir/targetFile'}
     *
     * @param targetFile the merge destination file, relative to {@link #targetDir()}
     * @param sourceFile the merge source file
     */
    void merge(Path targetFile, Path sourceFile);

    /**
     * Update entries in the specified file.<br>
     * Can only be applied for files which {@link FileContentHandler} supports it<br>
     * {@code setEntries(targetDir/file, entries) => targetDir/file'}
     *
     * @param file the file to be updated, relative to {@link #targetDir()}
     * @param entries the entries to be added or updated
     */
    void setEntries(Path file, List<String> entries);

    /**
     * Remove entries in the specified file.<br>
     * Can only be applied for files which {@link FileContentHandler} supports it<br>
     * {@code removeEntries(targetDir/file, entries) => targetDir/file'}
     *
     * @param file the file to be updated, relative to {@link #targetDir()}
     * @param entries the entries to be removed
     */
    void removeEntries(Path file, List<String> entries);
}
