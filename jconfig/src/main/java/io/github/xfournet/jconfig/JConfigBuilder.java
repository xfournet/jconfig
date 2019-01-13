package io.github.xfournet.jconfig;

import java.nio.file.Path;
import java.util.function.*;
import io.github.xfournet.jconfig.impl.JConfigImpl;
import io.github.xfournet.jconfig.jvm.JvmConfContentHandler;
import io.github.xfournet.jconfig.properties.PropertiesContentHandler;
import io.github.xfournet.jconfig.raw.RawFileContentHandler;

import static java.util.Objects.*;

@SuppressWarnings("WeakerAccess")
public class JConfigBuilder {
    private static final Predicate<Path> DEFAULT_PATH_FILTER = path -> !path.startsWith("META-INF");

    private static final Function<Path, FileContentHandler> DEFAULT_FILE_CONTENT_HANDLER_RESOLVER = path -> {
        String fileName = path.getFileName().toString();
        if ("jvm.conf".equals(fileName)) {
            return new JvmConfContentHandler();
        } else if (fileName.endsWith(".properties")) {
            return new PropertiesContentHandler();
        } else {
            return new RawFileContentHandler();
        }
    };

    private Predicate<Path> m_pathFilter = defaultPathFilter();
    private Function<Path, FileContentHandler> m_fileContentHandlerResolver = defaultFileContentHandlerResolver();

    private JConfigBuilder() {
    }

    /**
     * Specify a path filter to be used for filtering path in commands like diff, merge, ...
     *
     * @param pathFilter the predicate that indicates which path must be processed
     */
    public JConfigBuilder setPathFilter(Predicate<Path> pathFilter) {
        m_pathFilter = requireNonNull(pathFilter);
        return this;
    }

    /**
     * Specify the {@link FileContentHandler} resolver based on path
     *
     * @param fileContentHandlerResolver the resolver function
     */
    public JConfigBuilder setFileContentHandlerResolver(Function<Path, FileContentHandler> fileContentHandlerResolver) {
        m_fileContentHandlerResolver = requireNonNull(fileContentHandlerResolver);
        return this;
    }

    /**
     * Create a {@link JConfig} for the specified path.
     *
     * @param targetDir the target directory for commands
     * @return a new {@link JConfig} for the specified {@code targetDir}
     */
    public JConfig build(Path targetDir) {
        return new JConfigImpl(targetDir, m_pathFilter, m_fileContentHandlerResolver);
    }

    /**
     * @return a new {@link JConfigBuilder}
     */
    public static JConfigBuilder jConfigBuilder() {
        return new JConfigBuilder();
    }

    /**
     * @return the default path filter that ignore META-INF files (for JAR archives merge)
     */
    public static Predicate<Path> defaultPathFilter() {
        return DEFAULT_PATH_FILTER;
    }

    /**
     * @return the default {@link FileContentHandler} resolver function
     */
    public static Function<Path, FileContentHandler> defaultFileContentHandlerResolver() {
        return DEFAULT_FILE_CONTENT_HANDLER_RESOLVER;
    }
}
