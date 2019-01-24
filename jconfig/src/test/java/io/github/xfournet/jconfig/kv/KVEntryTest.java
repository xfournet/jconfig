package io.github.xfournet.jconfig.kv;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KVEntryTest {

    @Test
    public void testFilter() {
        String expected = "987";
        String value = KVEntry.EXPRESSION_TOKEN_BEGIN + "1234" + KVEntry.EXPRESSION_TOKEN_END;
        String filtered = KVEntry.filter(expr -> expected, value);

        assertThat(filtered).isEqualTo(expected);
    }

    @Test
    public void testNoopFilter() {
        String value = KVEntry.EXPRESSION_TOKEN_BEGIN + "1234" + KVEntry.EXPRESSION_TOKEN_END;
        String filtered = KVEntry.filter(expr -> null, value);

        assertThat(filtered).isEqualTo(value);
    }
}
