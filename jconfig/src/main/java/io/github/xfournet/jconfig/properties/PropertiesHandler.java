package io.github.xfournet.jconfig.properties;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import io.github.xfournet.jconfig.kv.KVConfHandler;
import io.github.xfournet.jconfig.kv.KVEntry;

public final class PropertiesHandler extends KVConfHandler<String> {

    public PropertiesHandler() {
        super(StandardCharsets.ISO_8859_1, PropertiesHandler::parse, PropertiesHandler::format, s -> s);
    }

    @Override
    public boolean canHandle(Path file) {
        return file.getFileName().toString().endsWith(".properties");
    }

    private static KVEntry<String> parse(String line) {
        String key = line;
        String value = "";
        int pos = line.indexOf("=");
        if (pos != -1) {
            key = line.substring(0, pos);
            value = line.substring(pos + 1);
        }

        return new KVEntry<>(key, value);
    }

    private static String format(KVEntry<String> entry) {
        // TODO escaping ?
        return entry.getKey() + "=" + entry.getValue();
    }
}
