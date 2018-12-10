package io.github.xfournet.jconfig;

import java.io.*;
import java.util.*;
import javax.annotation.*;

/**
 * An handler that permits to manipulate some configuration file.
 */
public interface FileContentHandler {

    /**
     * Apply a list of diff instruction.
     *
     * @param source the input where to read the source of the apply process
     * @param result the output where to write the result of the apply process
     * @param diff the {@link Diff} object to be applied
     */
    default void apply(@Nullable InputStream source, OutputStream result, Diff diff) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Generate the instructions that represent the difference between a file and a reference
     *
     * @param source the content from which to generate the diff
     * @param referenceSource the reference file to be used for the diff process
     * @return {@code null} if files are identical, else return the {@link Diff} object that can be then replayed with {@link #apply(InputStream, OutputStream, Diff)} method
     */
    @Nullable
    default Diff diff(InputStream source, @Nullable InputStream referenceSource) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Merge two inputs to a single output.
     *
     * @param source1 the first source of the merge that will be merged into the 2nd source
     * @param source2 the second source of the merge into which the first source will be merged
     * @param result the output where to write the result of the merge
     */
    default void merge(InputStream source1, InputStream source2, OutputStream result) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Update some entries.
     *
     * @param source the input where to read the source of the update process
     * @param result the output where to write the result of the update process
     * @param entries the list of entries to be updated
     */
    default void setEntries(InputStream source, OutputStream result, List<String> entries) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove some entries.
     *
     * @param source the input where to read the source of the remove process
     * @param result the output where to write the result of the remove process
     * @param entries the list of entries to be updated
     */
    default void removeEntries(InputStream source, OutputStream result, List<String> entries) throws IOException {
        throw new UnsupportedOperationException();
    }
}
