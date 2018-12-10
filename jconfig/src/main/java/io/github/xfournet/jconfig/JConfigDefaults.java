package io.github.xfournet.jconfig;

import java.nio.file.Path;
import java.util.function.*;
import io.github.xfournet.jconfig.jvm.JvmConfContentHandler;
import io.github.xfournet.jconfig.properties.PropertiesContentHandler;
import io.github.xfournet.jconfig.raw.RawFileContentHandler;

/**
 * Contains default behaviors that can be used for {@link JConfig}.
 */
public final class JConfigDefaults {

    /**
     * @return a default {@link FileContentHandler} resolver (with tuned text detection parameter) that can be used with {@link JConfig#newJConfig} methods
     */
    public static Function<Path, FileContentHandler> getDefaultFileHandlerResolver() {
        return path -> {
            String fileName = path.getFileName().toString();
            if (fileName.equals("jvm.conf")) {
                return new JvmConfContentHandler();
            } else if (fileName.endsWith(".properties")) {
                return new PropertiesContentHandler();
            } else {
                return new RawFileContentHandler();
            }
        };
    }

    private JConfigDefaults() {
    }
}
