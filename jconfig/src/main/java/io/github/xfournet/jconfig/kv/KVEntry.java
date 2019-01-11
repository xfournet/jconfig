package io.github.xfournet.jconfig.kv;

import java.util.*;
import java.util.function.*;

public class KVEntry<K> {
    public static final String VAR_TOKEN_BEGIN = "@{";
    public static final String VAR_TOKEN_END = "}";

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
        String filteredValue = value;
        int searchFrom = 0;
        while (searchFrom != -1) {
            searchFrom = -1;
            int begin = filteredValue.indexOf(VAR_TOKEN_BEGIN, searchFrom);
            if (begin != -1) {
                int end = filteredValue.indexOf(VAR_TOKEN_END, begin);
                if (end != -1) {
                    String varName = filteredValue.substring(begin + VAR_TOKEN_BEGIN.length(), end);
                    String filteredToken = resolveVar(varResolver, varName);
                    filteredValue = filteredValue.substring(0, begin) + filteredToken + filteredValue.substring(end + VAR_TOKEN_END.length());

                    // next search to be done after the resolved value so:
                    // - variable are not recursively resolved, if needed this should done in the variable resolver itself
                    // - variable resolver could process unresolved values untouched by returning {@code VAR_TOKEN_BEGIN + varName + VAR_TOKEN_END}
                    searchFrom = begin + filteredToken.length();
                }
            }
        }
        return filteredValue;
    }

    private static String resolveVar(UnaryOperator<String> varResolver, String key) {
        String value = varResolver.apply(key);
        if (value == null) {
            throw new IllegalArgumentException("Variable not found: " + key);
        }
        return value;
    }
}
