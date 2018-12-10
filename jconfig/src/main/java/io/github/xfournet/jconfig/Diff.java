package io.github.xfournet.jconfig;

import java.util.*;
import javax.annotation.*;

/**
 * Represent a difference between two files.
 */
public class Diff {
    private final boolean m_overwrite;
    @Nullable
    private final String m_encoding;
    private final List<String> m_lines;

    public Diff(boolean overwrite, @Nullable String encoding, List<String> lines) {
        m_overwrite = overwrite;
        m_encoding = encoding;
        m_lines = lines;
    }

    /**
     * @return {@code true} if the original file is completely overwritten by the difference or {@code false} when the difference must be merged into the original file
     */
    public boolean isOverwrite() {
        return m_overwrite;
    }

    /**
     * @return an optional information that indicate how {@link #getLines()} are encoded.
     */
    @Nullable
    public String getEncoding() {
        return m_encoding;
    }

    /**
     * @return the lines that represent the content of the difference
     */
    public List<String> getLines() {
        return m_lines;
    }
}
