package io.github.xfournet.jconfig.kv;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import javax.annotation.*;
import io.github.xfournet.jconfig.Diff;
import io.github.xfournet.jconfig.FileContentHandler;

public abstract class KVConfContentHandler<K> implements FileContentHandler {
    private final Charset m_charset;
    private final Function<String, KVEntry<K>> m_entryParser;
    private final Function<KVEntry<K>, String> m_entryFormatter;
    private final Function<K, String> m_keyFormatter;

    protected KVConfContentHandler(Charset charset, Function<String, KVEntry<K>> entryParser, Function<KVEntry<K>, String> entryFormatter,
                                   Function<K, String> keyFormatter) {
        m_charset = charset;
        m_entryParser = entryParser;
        m_entryFormatter = entryFormatter;
        m_keyFormatter = keyFormatter;
    }

    @Override
    public void apply(@Nullable InputStream source, OutputStream result, Diff diff) throws IOException {
        KVConf<K> conf;
        if (diff.isOverwrite()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(result, m_charset))) {
                for (String line : diff.getLines()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } else {
            conf = readConf(source);
            conf.apply(diff.getLines(), m_entryParser);
            writeConf(result, conf);
        }
    }

    @Override
    public Diff diff(InputStream source, @Nullable InputStream referenceSource) throws IOException {
        boolean overwrite = referenceSource == null;

        List<String> lines;
        if (overwrite) {
            lines = new BufferedReader(new InputStreamReader(source, m_charset)).lines().collect(Collectors.toList());
        } else {
            lines = generateDiff(source, referenceSource);
            if (lines.isEmpty()) {
                return null;
            }
        }

        return new Diff(overwrite, null, lines);
    }

    private List<String> generateDiff(InputStream file, InputStream referenceFile) throws IOException {
        KVConf<K> conf = readConf(file);
        KVConf<K> refConf = readConf(referenceFile);

        return conf.diffFrom(refConf, m_entryFormatter, m_keyFormatter);
    }

    @Override
    public void merge(InputStream contentToMerge, InputStream sourceToUpdate, OutputStream result) throws IOException {
        KVConf<K> confToMerge = readConf(contentToMerge);
        KVConf<K> confToUpdate = readConf(sourceToUpdate);

        confToUpdate.mergeWith(confToMerge);

        writeConf(result, confToUpdate);
    }

    @Override
    public void setEntries(InputStream source, OutputStream result, List<String> entries) throws IOException {
        KVConf<K> conf = readConf(source);

        entries.forEach(entry -> {
            KVEntry<K> parsedEntry = m_entryParser.apply(entry);
            conf.setEntry(parsedEntry);
        });

        writeConf(result, conf);
    }

    @Override
    public void removeEntries(InputStream source, OutputStream result, List<String> entries) throws IOException {
        KVConf<K> conf = readConf(source);

        entries.forEach(entry -> {
            KVEntry<K> parsedEntry = m_entryParser.apply(entry);
            conf.removeEntry(parsedEntry.getKey());
        });

        writeConf(result, conf);
    }

    @Override
    public void filter(InputStream source, OutputStream result, Function<String, String> variableResolver) throws IOException {
        KVConf<K> conf = readConf(source);
        conf.filter(variableResolver);
        writeConf(result, conf);
    }

    private KVConf<K> readConf(@Nullable InputStream source) throws IOException {
        return KVConf.readConf(source, m_charset, m_entryParser);
    }

    private void writeConf(OutputStream result, KVConf<K> conf) throws IOException {
        conf.write(result, m_charset, m_entryFormatter);
    }
}
