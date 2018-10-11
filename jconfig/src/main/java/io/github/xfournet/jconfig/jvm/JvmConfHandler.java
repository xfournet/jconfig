package io.github.xfournet.jconfig.jvm;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.regex.*;
import io.github.xfournet.jconfig.kv.KVConfHandler;
import io.github.xfournet.jconfig.kv.KVEntry;

public final class JvmConfHandler extends KVConfHandler<JvmConfEntryKey> {

    public JvmConfHandler() {
        super(Charset.defaultCharset(), JvmConfHandler::parse, JvmConfHandler::format, JvmConfHandler::formatKey);
    }

    @Override
    public boolean canHandle(Path file) {
        return file.getFileName().toString().equals("jvm.conf");
    }

    private static KVEntry<JvmConfEntryKey> parse(String line) {
        for (JvmConfEntryType entryType : JvmConfEntryType.values()) {
            Matcher matcher = entryType.matcher(line);
            if (matcher.matches()) {
                String key = matcher.group(entryType.isKeyFirst() ? 1 : 2);
                String value = matcher.group(entryType.isKeyFirst() ? 2 : 1);
                return new KVEntry<>(new JvmConfEntryKey(entryType, key), value);
            }
        }
        throw new IllegalArgumentException("Unrecognized jvm.conf option: " + line);
    }

    private static String format(KVEntry<JvmConfEntryKey> entry) {
        JvmConfEntryKey key = entry.getKey();
        JvmConfEntryType type = key.getType();

        return type.format(key.getKey(), entry.getValue());
    }

    private static String formatKey(JvmConfEntryKey key) {
        JvmConfEntryType type = key.getType();
        return type.format(key.getKey(), "");
    }
}
