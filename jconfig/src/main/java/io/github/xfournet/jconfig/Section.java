package io.github.xfournet.jconfig;

import java.util.*;
import javax.annotation.*;

public class Section {
    public enum Mode {
        APPLY, OVERWRITE, DELETE
    }

    private final String m_filePath;
    private final Mode m_mode;
    @Nullable
    private final String m_encoding;
    private final List<String> m_lines;

    public Section(String filePath, Mode mode, @Nullable String encoding, List<String> lines) {
        m_filePath = filePath;
        m_mode = mode;
        m_encoding = encoding;
        m_lines = lines;
    }

    public String getFilePath() {
        return m_filePath;
    }

    public Mode getMode() {
        return m_mode;
    }

    @Nullable
    public String getEncoding() {
        return m_encoding;
    }

    public List<String> getLines() {
        return m_lines;
    }
}
