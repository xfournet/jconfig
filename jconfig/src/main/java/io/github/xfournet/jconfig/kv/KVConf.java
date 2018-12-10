package io.github.xfournet.jconfig.kv;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import javax.annotation.*;

class KVConf<K> {
    private static final String COMMENT_MARK = "#";

    private final List<KVEntry<K>> m_entries;
    private final Map<K, KVEntry<K>> m_entriesByKey;

    static <K> KVConf<K> readConf(@Nullable InputStream input, Charset charset, Function<String, KVEntry<K>> entryParser) throws IOException {
        List<KVEntry<K>> entries = new ArrayList<>();
        Map<K, KVEntry<K>> entriesByKey = new HashMap<>();

        List<String> comments = new ArrayList<>();

        if (input != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
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
                }
            }
        }

        return new KVConf<>(entries, entriesByKey);
    }

    private KVConf(List<KVEntry<K>> entries, Map<K, KVEntry<K>> entriesByKey) {
        m_entries = entries;
        m_entriesByKey = entriesByKey;
    }

    void mergeWith(KVConf<K> source) {
        source.m_entries.forEach(this::setEntry);
    }

    void write(OutputStream output, Charset charset, Function<KVEntry<K>, String> entryFormatter) throws IOException {
        boolean firstLine = true;
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, charset))) {
            for (KVEntry<K> entry : m_entries) {
                for (String comment : entry.getComments()) {
                    if (!(firstLine && comment.isEmpty())) {
                        writer.write(comment);
                        writer.newLine();
                        firstLine = false;
                    }
                }
                writer.write(entryFormatter.apply(entry));
                writer.newLine();
                firstLine = false;
            }
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

    List<String> diffFrom(KVConf<K> refConf, Function<KVEntry<K>, String> entryFormatter, Function<K, String> keyFormatter) {
        List<String> lines = new ArrayList<>();

        // generate remove instructions
        lines.addAll( //
                      refConf.m_entriesByKey.keySet().stream() //
                              .filter(k -> !m_entriesByKey.containsKey(k)) //
                              .map(k -> "-remove " + keyFormatter.apply(k)) //
                              .sorted() //
                              .collect(Collectors.toList()));

        // generate set instructions
        lines.addAll( //
                      m_entries.stream() //
                              .filter(entry -> {
                                  KVEntry<K> refEntry = refConf.m_entriesByKey.get(entry.getKey());
                                  return refEntry == null || !Objects.equals(entry.getValue(), refEntry.getValue());
                              }) //
                              .flatMap(entry -> {
                                  List<String> entryLines = new ArrayList<>(entry.getComments());
                                  String entryStr = entryFormatter.apply(entry);
                                  if (entryStr.startsWith("-remove") || entryStr.startsWith("-set")) {
                                      entryStr = "-set " + entryStr;
                                  }
                                  entryLines.add(entryStr);
                                  return entryLines.stream();
                              }) //
                              .collect(Collectors.toList()));

        return lines;
    }
}
