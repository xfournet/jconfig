package io.github.xfournet.jconfig.kv;

import java.util.*;
import java.util.function.*;

public class KVEntry<K> {
    public static final String EXPRESSION_TOKEN_BEGIN = "@{";
    public static final String EXPRESSION_TOKEN_END = "}";

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

    void filter(UnaryOperator<String> expressionProcessor) {
        m_value = filter(expressionProcessor, m_value);
    }

    private static String filter(UnaryOperator<String> expressionProcessor, String value) {
        String filteredValue = value;
        int searchFrom = 0;
        while (searchFrom != -1) {
            searchFrom = -1;
            int begin = filteredValue.indexOf(EXPRESSION_TOKEN_BEGIN, searchFrom);
            if (begin != -1) {
                int end = filteredValue.indexOf(EXPRESSION_TOKEN_END, begin);
                if (end != -1) {
                    String expression = filteredValue.substring(begin + EXPRESSION_TOKEN_BEGIN.length(), end);
                    String filteredToken = processExpression(expressionProcessor, expression);
                    filteredValue = filteredValue.substring(0, begin) + filteredToken + filteredValue.substring(end + EXPRESSION_TOKEN_END.length());

                    // next search to be done after the processed expression so:
                    // - expression are not recursively processed, if needed this should done in the expression processor itself
                    // - expression processor could let expression untouched by returning {@code EXPRESSION_TOKEN_BEGIN + expression + EXPRESSION_TOKEN_END}
                    searchFrom = begin + filteredToken.length();
                }
            }
        }
        return filteredValue;
    }

    private static String processExpression(UnaryOperator<String> expressionProcessor, String expression) {
        String value = expressionProcessor.apply(expression);
        if (value == null) {
            throw new IllegalArgumentException("Expression not processed: " + expression);
        }
        return value;
    }
}
