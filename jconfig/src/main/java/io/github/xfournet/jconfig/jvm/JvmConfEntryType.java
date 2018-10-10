package io.github.xfournet.jconfig.jvm;

import java.util.regex.*;
import javax.annotation.*;

enum JvmConfEntryType {
    PROPERTY(Pattern.compile("^-D(.+?)(?:=(.*))?$")) {
        @Override
        public String format(String key, @Nullable String value) {
            return "-D" + key + (value != null ? "=" + value : "");
        }
    },

    XX_PROPERTY_BOOLEAN(Pattern.compile("^-XX:([+-])(.+)$"), false) {
        @Override
        public String format(String key, String value) {
            return "-XX:" + value + key;
        }
    },

    XX_PROPERTY(Pattern.compile("^-XX:(.+?)=(.*)$")) {
        @Override
        public String format(String key, String value) {
            return "-XX:" + key + "=" + value;
        }
    },

    ADD_EXPORTS_EQUALS(Pattern.compile("^--add-exports=(.+?)=(.+)$")) {
        @Override
        public String format(String key, String value) {
            return "--add-exports=" + key + "=" + value;
        }
    },

    ADD_READS_EQUALS(Pattern.compile("^--add-reads=(.+?)=(.+)$")) {
        @Override
        public String format(String key, String value) {
            return "--add-reads=" + key + "=" + value;
        }
    },

    ADD_OPENS_EQUALS(Pattern.compile("^--add-opens=(.+?)=(.+)$")) {
        @Override
        public String format(String key, String value) {
            return "--add-opens=" + key + "=" + value;
        }
    },

    OPT_EQUALS(Pattern.compile("^-(.+?)=(.+)$")) {
        @Override
        public String format(String key, String value) {
            return "-" + key + "=" + value;
        }
    },

    OPT_COLUMNS(Pattern.compile("^-(.+?):(.+)$")) {
        @Override
        public String format(String key, String value) {
            return "-" + key + ":" + value;
        }
    },

    OPT_NUMBERS(Pattern.compile("^-(.+?)([0-9].*)$")) {
        @Override
        public String format(String key, String value) {
            return "-" + key + value;
        }
    },

    OPT_OTHER(Pattern.compile("^-(.+)(.*)$")) {
        @Override
        public String format(String key, String value) {
            return "-" + key + value;
        }
    };

    private final Pattern m_pattern;
    private final boolean m_keyFirst;

    JvmConfEntryType(Pattern pattern) {
        this(pattern, true);
    }

    JvmConfEntryType(Pattern pattern, boolean keyFirst) {
        m_pattern = pattern;
        m_keyFirst = keyFirst;
    }

    public Matcher matcher(String line) {
        return m_pattern.matcher(line);
    }

    public boolean isKeyFirst() {
        return m_keyFirst;
    }

    public abstract String format(String key, String value);
}
