package io.github.xfournet.jconfig.kv;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import io.github.xfournet.jconfig.FileHandler;

public abstract class KVConfHandler<K> implements FileHandler {
    private final Charset m_charset;
    private final Function<String, KVEntry<K>> m_entryParser;
    private final Function<KVEntry<K>, String> m_entryFormatter;

    protected KVConfHandler(Charset charset, Function<String, KVEntry<K>> entryParser, Function<KVEntry<K>, String> entryFormatter) {
        m_charset = charset;
        m_entryParser = entryParser;
        m_entryFormatter = entryFormatter;
    }

    @Override
    public Charset getCharset() {
        return m_charset;
    }

    @Override
    public void mergeFiles(Path source1, Path source2, Path destination) {
        KVConf<K> conf1 = readConf(source1);
        KVConf<K> conf2 = readConf(source2);

        conf1.merge(conf2);

        writeConf(destination, conf1);
    }

    @Override
    public void apply(Path file, List<String> instructions, Path destination) {
        KVConf<K> conf = readConf(file);

        conf.apply(instructions, m_entryParser);

        writeConf(destination, conf);
    }

    @Override
    public void setEntry(Path file, String entry, Path destination) {
        KVConf<K> conf = readConf(file);

        KVEntry<K> parsedEntry = m_entryParser.apply(entry);
        conf.setEntry(parsedEntry);

        writeConf(destination, conf);
    }

    @Override
    public void removeEntry(Path file, String entry, Path destination) {
        KVConf<K> conf = readConf(file);

        KVEntry<K> parsedEntry = m_entryParser.apply(entry);
        conf.removeEntry(parsedEntry.getKey());

        writeConf(destination, conf);
    }

    @Override
    public void normalize(Path file, Path destination) {
        KVConf<K> conf = readConf(file);
        writeConf(destination, conf);
    }

    private KVConf<K> readConf(Path file) {
        return KVConf.readConf(file, m_charset, m_entryParser);
    }

    private void writeConf(Path file, KVConf<K> conf) {
        conf.write(file, m_charset, m_entryFormatter);
    }
}
