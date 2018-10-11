package io.github.xfournet.jconfig.impl;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.stream.*;
import javax.annotation.*;
import io.github.xfournet.jconfig.FileHandler;
import io.github.xfournet.jconfig.JConfig;
import io.github.xfournet.jconfig.jvm.JvmConfHandler;
import io.github.xfournet.jconfig.properties.PropertiesHandler;
import io.github.xfournet.jconfig.raw.RawFileHandler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class JConfigImpl implements JConfig {
    private static final Charset INI_CHARSET = UTF_8;
    private static final Pattern SECTION_MARKER = Pattern.compile("^\\[(.+)]( +#.*)?$");

    private final List<FileHandler> m_fileHandlers = Arrays.asList(new JvmConfHandler(), new PropertiesHandler(), new RawFileHandler());

    @Override
    public void mergeFiles(Path source1, Path source2, Path destination) {
        try (Transaction tx = new Transaction()) {
            retrieveFileHandler(source1).mergeFiles(source1, source2, tx.getOutputFile(destination));
            tx.commit();
        }
    }

    @Override
    public void setEntries(Path file, List<String> entries) {
        FileHandler fileHandler = retrieveFileHandler(file);
        try (Transaction tx = new Transaction()) {
            fileHandler.setEntries(file, tx.getOutputFile(file), entries);
            tx.commit();
        }
    }

    @Override
    public void removeEntries(Path file, List<String> entries) {
        FileHandler fileHandler = retrieveFileHandler(file);
        try (Transaction tx = new Transaction()) {
            fileHandler.removeEntries(file, tx.getOutputFile(file), entries);
            tx.commit();
        }
    }

    @Override
    public void normalize(Path file) {
        FileHandler fileHandler = retrieveFileHandler(file);
        try (Transaction tx = new Transaction()) {
            fileHandler.normalize(file, tx.getOutputFile(file));
            tx.commit();
        }
    }

    @Override
    public void apply(Path iniFile, Path targetDir) {
        try (Transaction tx = new Transaction()) {
            Map<String, List<String>> sections = parseSections(iniFile);
            sections.forEach((section, lines) -> processSection(tx, section, lines, targetDir));

            tx.commit();
        }
    }

    private static boolean isContentLine(String line) {
        // traditional .ini file comment mark is ';' , add support of traditional '#' also
        return !line.isEmpty() && !(line.startsWith("#") || line.startsWith(";"));
    }

    private Map<String, List<String>> parseSections(Path iniFile) {
        Map<String, List<String>> sections = new LinkedHashMap<>();

        try (Stream<String> lines = Files.lines(iniFile, INI_CHARSET)) {
            AtomicReference<String> currentSectionRef = new AtomicReference<>();
            List<String> sectionLines = new ArrayList<>();

            lines.map(String::trim).forEach(line -> {
                final String currentSection = currentSectionRef.get();

                Matcher matcher = SECTION_MARKER.matcher(line);
                if (matcher.matches()) {
                    if (currentSection != null) {
                        sections.put(currentSection, trimSection(sectionLines));
                    }
                    currentSectionRef.set(matcher.group(1));
                    sectionLines.clear();
                } else {
                    if (currentSection != null) {
                        sectionLines.add(line);
                    } else {
                        if (isContentLine(line)) {
                            throw new IllegalStateException("Content outside INI section in " + iniFile);
                        }
                    }
                }
            });

            String currentSection = currentSectionRef.get();
            if (currentSection != null) {
                sections.put(currentSection, trimSection(sectionLines));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return sections;
    }

    private List<String> trimSection(List<String> lines) {
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        return new ArrayList<>(lines);
    }

    private void processSection(Transaction tx, String section, List<String> lines, Path target) {
        String[] elements = section.split(" +");
        if (elements.length < 1 || elements.length > 2) {
            throw new IllegalArgumentException("Invalid INI section : " + section);
        }

        String fileName = elements[0];
        boolean apply = false;
        boolean delete = false;
        boolean base64 = false;
        Charset charset = null;
        if (elements.length > 1) {
            String element = elements[1];
            if ("apply".equalsIgnoreCase(element)) {
                apply = true;
            } else if ("delete".equalsIgnoreCase(element)) {
                delete = true;
            } else if ("@base64".equalsIgnoreCase(element)) {
                base64 = true;
            } else if (element.startsWith("@")) {
                charset = Charset.forName(element.substring(1));
            }
        }

        Path file = target.resolve(fileName);
        if (delete) {
            handleDelete(tx, file, lines);
        } else {
            if (apply) {
                handleApply(tx, file, lines);
            } else {
                handleOverride(tx, file, lines, base64, charset);
            }
        }
    }

    private void handleDelete(Transaction tx, Path file, List<String> lines) {
        if (lines.stream().anyMatch(JConfigImpl::isContentLine)) {
            throw new IllegalStateException("Delete section contains content");
        }

        tx.deleteFile(file);
    }

    private void handleOverride(Transaction tx, Path file, List<String> lines, boolean base64, @Nullable Charset charset) {
        Path outputFile = tx.getOutputFile(file);
        try {
            Files.createDirectories(outputFile.getParent());

            if (base64) {
                Base64.Decoder decoder = Base64.getDecoder();
                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(outputFile, TRUNCATE_EXISTING))) {
                    for (String line : lines) {
                        out.write(decoder.decode(line));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                if (charset == null) {
                    charset = retrieveFileHandler(file).getCharset();
                }
                Files.write(outputFile, lines, charset);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void handleApply(Transaction tx, Path file, List<String> lines) {
        FileHandler fileHandler = retrieveFileHandler(file);
        fileHandler.apply(file, lines, tx.getOutputFile(file));
    }

    private FileHandler retrieveFileHandler(Path file) {
        for (FileHandler fileHandler : m_fileHandlers) {
            if (fileHandler.canHandle(file)) {
                return fileHandler;
            }
        }

        throw new IllegalArgumentException("No handler was found for file: " + file);
    }

    private static class Transaction implements AutoCloseable {
        private final long m_id = System.currentTimeMillis();
        private final List<Runnable> m_actions = new ArrayList<>();
        private boolean m_commit = false;

        Path getOutputFile(Path file) {
            Path tmpFile = Paths.get(file.toString() + "." + m_id + ".tmp");
            m_actions.add(new TempFile(file, tmpFile));
            try {
                Files.createDirectories(tmpFile.getParent());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return tmpFile;
        }

        void deleteFile(Path file) {
            m_actions.add(() -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        void commit() {
            m_commit = true;
        }

        @Override
        public void close() {
            m_actions.forEach(Runnable::run);
        }

        private class TempFile implements Runnable {
            private final Path m_file;
            private final Path m_tmpFile;

            TempFile(Path file, Path tmpFile) {
                m_file = file;
                m_tmpFile = tmpFile;
            }

            @Override
            public void run() {
                try {
                    if (m_commit) {
                        Files.move(m_tmpFile, m_file, REPLACE_EXISTING, ATOMIC_MOVE);
                    } else {
                        Files.deleteIfExists(m_tmpFile);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }
}
