package io.github.xfournet.jconfig.properties;

import io.github.xfournet.jconfig.kv.KVConfContentHandler;
import io.github.xfournet.jconfig.kv.KVEntry;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.function.UnaryOperator.*;

// TODO escaping and multiline (difficult ?) should be implemented for better accuracy
public final class PropertiesContentHandler extends KVConfContentHandler<String> {

    public PropertiesContentHandler() {
        super(ISO_8859_1, PropertiesContentHandler::parse, PropertiesContentHandler::format, identity());
    }

    private static KVEntry<String> parse(String line) {
        String key = line;
        String value = "";
        int posEqual = line.indexOf('=');
        int posColon = line.indexOf(':');
        int pos;
        if (posEqual != -1 && posColon != -1) {
            pos = Math.min(posColon, posEqual);
        } else if (posEqual != -1) {
            pos = posEqual;
        } else if (posColon != -1) {
            pos = posColon;
        } else {
            pos = -1;
        }

        if (pos != -1) {
            key = line.substring(0, pos).trim();
            value = line.substring(pos + 1).trim();
        }

        return new KVEntry<>(key, value);
    }

    private static String format(KVEntry<String> entry) {
        return entry.getKey() + "=" + entry.getValue();
    }
}
