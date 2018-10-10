package io.github.xfournet.jconfig.kv;

import java.util.*;

public class KVEntry<K> {
    private final K m_key;
    private final String m_value;
    private final List<String> m_comments = new ArrayList<>();

    public KVEntry(K key, String value) {
        m_key = key;
        m_value = value;
    }

    public K getKey() {
        return m_key;
    }

    public String getValue() {
        return m_value;
    }

    List<String> getComments() {
        return m_comments;
    }

    void setComments(List<String> comments) {
        m_comments.clear();
        m_comments.addAll(comments);
    }
}
