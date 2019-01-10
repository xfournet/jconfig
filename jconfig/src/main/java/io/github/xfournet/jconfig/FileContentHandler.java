package io.github.xfournet.jconfig;

import java.io.*;
import java.util.*;
import java.util.function.*;
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
     * @throws IOException in case an error occurs on a stream
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
     * @throws IOException in case an error occurs on a stream
     */
    @Nullable
    default Diff diff(InputStream source, @Nullable InputStream referenceSource) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Merge two inputs to a single output.
     *
     * @param contentToMerge the content that will be merged into {@code sourceToUpdate}
     * @param sourceToUpdate the source into which {@code contentToMerge} will be merged
     * @param result the output where to write the result of the merge
     * @throws IOException in case an error occurs on a stream
     */
    default void merge(InputStream contentToMerge, InputStream sourceToUpdate, OutputStream result) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Update some entries.
     *
     * @param source the input where to read the source of the update process
     * @param result the output where to write the result of the update process
     * @param entries the list of entries to be updated
     * @throws IOException in case an error occurs on a stream
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
     * @throws IOException in case an error occurs on a stream
     */
    default void removeEntries(InputStream source, OutputStream result, List<String> entries) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Update content with specified variables.
     *
     * @param source the input where to read the source of the filtering process
     * @param result the output where to write the result of the filtering process
     * @param variableResolver a function that permit to resolve the variable value.
     * {@code Map} or {@code Properties} can be easily use here thanks to function reference, eg {@code varMap::get} or {@code varProps::getProperty}
     * @throws IOException in case an error occurs on a stream
     */
    default void filter(InputStream source, OutputStream result, Function<String, String> variableResolver) throws IOException {
        throw new UnsupportedOperationException();
    }
}
