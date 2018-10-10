package io.github.xfournet.jconfig.kv;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

class KVConf<K> {
    private static final String COMMENT_MARK = "#";

    private final List<KVEntry<K>> m_entries;
    private final Map<K, KVEntry<K>> m_entriesByKey;

    static <K> KVConf<K> readConf(Path file, Charset charset, Function<String, KVEntry<K>> entryParser) {
        List<KVEntry<K>> entries = new ArrayList<>();
        Map<K, KVEntry<K>> entriesByKey = new HashMap<>();
        if (Files.exists(file)) {

            try (Stream<String> lines = Files.lines(file, charset)) {
                List<String> comments = new ArrayList<>();

                lines.forEach(line -> {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith(COMMENT_MARK)) {
                        comments.add(line);
                    } else {
                        KVEntry<K> entry = entryParser.apply(line);
                        entry.setComments(comments);

                        K key = entry.getKey();

                        // remove previous existing entry if exists
                        KVEntry<K> existingEntry = entriesByKey.remove(key);
                        if (existingEntry != null) {
                            entries.remove(existingEntry);
                        }

                        // add parsed entry
                        entries.add(entry);
                        entriesByKey.put(key, entry);

                        // comments has been associated to this entry, clear them for next round
                        comments.clear();
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return new KVConf<>(entries, entriesByKey);
    }

    private KVConf(List<KVEntry<K>> entries, Map<K, KVEntry<K>> entriesByKey) {
        m_entries = entries;
        m_entriesByKey = entriesByKey;
    }

    void merge(KVConf<K> source) {
        source.m_entries.forEach(this::setEntry);
    }

    void write(Path destination, Charset charset, Function<KVEntry<K>, String> entryFormatter) {
        List<String> lines = new ArrayList<>();
        for (KVEntry<K> entry : m_entries) {
            lines.addAll(entry.getComments());
            lines.add(entryFormatter.apply(entry));
        }

        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, lines, charset);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void setEntry(KVEntry<K> entry) {
        K key = entry.getKey();

        // remove previous existing entry if exists
        int index = m_entries.size();
        KVEntry<K> existingEntry = m_entriesByKey.remove(key);
        if (existingEntry != null) {
            index = m_entries.indexOf(existingEntry);
            m_entries.remove(index);

            if (entry.getComments().isEmpty() && !existingEntry.getComments().isEmpty()) {
                entry.setComments(existingEntry.getComments());
            }
        }

        // add entry
        m_entries.add(index, entry);
        m_entriesByKey.put(key, entry);
    }

    void removeEntry(K key) {
        KVEntry<K> existingEntry = m_entriesByKey.remove(key);
        if (existingEntry != null) {
            int index = m_entries.indexOf(existingEntry);
            m_entries.remove(existingEntry);
            if (!existingEntry.getComments().isEmpty() && index < m_entries.size()) {
                KVEntry<K> nextEntry = m_entries.get(index);
                if (nextEntry.getComments().isEmpty()) {
                    nextEntry.setComments(existingEntry.getComments());
                }
            }
        }
    }

    void apply(List<String> instructions, Function<String, KVEntry<K>> entryParser) {
        List<String> comments = new ArrayList<>();

        for (String instruction : instructions) {
            if (instruction.isEmpty() || instruction.startsWith(COMMENT_MARK)) {
                comments.add(instruction);
            } else {

                int index = instruction.indexOf(" ");
                if (index == -1) {
                    index = instruction.length();
                }

                String word = instruction.substring(0, index);

                boolean set;
                if ("-set".equals(word)) {
                    set = true;
                } else if ("-remove".equals(word)) {
                    set = false;
                } else {
                    set = true;
                    index = -1;
                }

                if (index < instruction.length()) {
                    index++;
                }

                String entry = instruction.substring(index);
                KVEntry<K> parsedEntry = entryParser.apply(entry);
                parsedEntry.setComments(comments);
                comments.clear();
                if (set) {
                    setEntry(parsedEntry);
                } else {
                    removeEntry(parsedEntry.getKey());
                }
            }
        }
    }
}
