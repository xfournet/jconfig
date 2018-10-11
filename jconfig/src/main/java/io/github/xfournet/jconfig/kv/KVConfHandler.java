package io.github.xfournet.jconfig.kv;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import javax.annotation.*;
import io.github.xfournet.jconfig.FileHandler;
import io.github.xfournet.jconfig.Section;

import static io.github.xfournet.jconfig.Section.Mode.*;

public abstract class KVConfHandler<K> implements FileHandler {
    private final Charset m_charset;
    private final Function<String, KVEntry<K>> m_entryParser;
    private final Function<KVEntry<K>, String> m_entryFormatter;
    private final Function<K, String> m_keyFormatter;

    protected KVConfHandler(Charset charset, Function<String, KVEntry<K>> entryParser, Function<KVEntry<K>, String> entryFormatter,
                            Function<K, String> keyFormatter) {
        m_charset = charset;
        m_entryParser = entryParser;
        m_entryFormatter = entryFormatter;
        m_keyFormatter = keyFormatter;
    }

    @Override
    public Charset getCharset() {
        return m_charset;
    }

    @Override
    public void apply(Path file, List<String> instructions, Path destination) {
        KVConf<K> conf = readConf(file);

        conf.apply(instructions, m_entryParser);

        writeConf(destination, conf);
    }

    @Override
    public Section diff(Path file, String fileName, @Nullable Path referenceFile) {
        Section.Mode mode;
        List<String> lines;

        if (referenceFile != null) {
            mode = APPLY;
            lines = generateDiff(file, referenceFile);
        } else {
            // no reference file, don't need to generate a diff
            mode = OVERWRITE;
            try {
                lines = Files.readAllLines(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return new Section(fileName, mode, null, lines);
    }

    private List<String> generateDiff(Path file, Path referenceFile) {
        KVConf<K> conf = readConf(file);
        KVConf<K> refConf = readConf(referenceFile);

        return conf.diffFrom(refConf, m_entryFormatter, m_keyFormatter);
    }

    @Override
    public void mergeFiles(Path source1, Path source2, Path destination) {
        KVConf<K> conf1 = readConf(source1);
        KVConf<K> conf2 = readConf(source2);

        conf1.merge(conf2);

        writeConf(destination, conf1);
    }

    @Override
    public void setEntries(Path file, Path destination, List<String> entries) {
        KVConf<K> conf = readConf(file);

        entries.forEach(entry -> {
            KVEntry<K> parsedEntry = m_entryParser.apply(entry);
            conf.setEntry(parsedEntry);
        });

        writeConf(destination, conf);
    }

    @Override
    public void removeEntries(Path file, Path destination, List<String> entries) {
        KVConf<K> conf = readConf(file);

        entries.forEach(entry -> {
            KVEntry<K> parsedEntry = m_entryParser.apply(entry);
            conf.removeEntry(parsedEntry.getKey());
        });

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
