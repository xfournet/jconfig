package io.github.xfournet.jconfig.jvm;

final class JvmConfEntryKey {
    private final JvmConfEntryType m_type;
    private final String m_key;

    JvmConfEntryKey(JvmConfEntryType type, String key) {
        m_type = type;
        m_key = key;
    }

    JvmConfEntryType getType() {
        return m_type;
    }

    String getKey() {
        return m_key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JvmConfEntryKey that = (JvmConfEntryKey) o;
        return m_type == that.m_type && m_key.equals(that.m_key);
    }

    @Override
    public int hashCode() {
        int result = m_type.hashCode();
        result = 31 * result + m_key.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return m_type + " " + m_key;
    }
}
