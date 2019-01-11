package io.github.xfournet.jconfig.kv;

import java.util.*;
import java.util.function.*;

public class KVEntry<K> {
    private final K m_key;
    private String m_value;
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

    void filter(UnaryOperator<String> variableResolver) {
        m_value = filter(variableResolver, m_value);
    }

    private static String filter(UnaryOperator<String> varResolver, String value) {
        String currentValue = value;
        String lastValue;
        do {
            lastValue = currentValue;
            int pos1 = currentValue.indexOf("@{");
            if (pos1 != -1) {
                int pos2 = currentValue.indexOf('}', pos1);
                if (pos2 != -1) {
                    currentValue = currentValue.substring(0, pos1) + //
                            resolveVar(varResolver, currentValue.substring(pos1 + 2, pos2)) + //
                            currentValue.substring(pos2 + 1);
                }
            }
        } while (!currentValue.equals(lastValue));
        return currentValue;
    }

    private static String resolveVar(UnaryOperator<String> varResolver, String key) {
        String value = varResolver.apply(key);
        if (value == null) {
            throw new IllegalArgumentException("Variable not found: " + key);
        }
        return value;
    }
}
