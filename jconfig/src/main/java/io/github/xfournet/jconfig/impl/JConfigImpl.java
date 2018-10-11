package io.github.xfournet.jconfig.impl;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;
import javax.annotation.*;
import io.github.xfournet.jconfig.FileHandler;
import io.github.xfournet.jconfig.JConfig;
import io.github.xfournet.jconfig.Section;
import io.github.xfournet.jconfig.jvm.JvmConfHandler;
import io.github.xfournet.jconfig.properties.PropertiesHandler;
import io.github.xfournet.jconfig.raw.RawFileHandler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.*;
import static java.util.Collections.*;

public class JConfigImpl implements JConfig {
    private static final Charset INI_CHARSET = UTF_8;
    private static final Pattern SECTION_MARKER = Pattern.compile("^\\[(.+)]( +#.*)?$");

    private final List<FileHandler> m_fileHandlers = Arrays.asList(new JvmConfHandler(), new PropertiesHandler(), new RawFileHandler());

    @Override
    public void apply(Path targetDir, Path iniFile) {
        try (Transaction tx = new Transaction()) {
            List<Section> sections = parseSections(iniFile);
            sections.forEach(section -> processSection(tx, section, targetDir));

            tx.commit();
        }
    }

    @Override
    public void diff(Path directory, Path referenceDir, Predicate<String> pathFilter, Path diffFile) {
        try (Transaction tx = new Transaction()) {
            List<Section> sections = generateSections(directory, referenceDir, pathFilter);
            writeSections(sections, tx.getOutputFile(diffFile));

            tx.commit();
        }
    }

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

    private static boolean isContentLine(String line) {
        // traditional .ini file comment mark is ';' , add support of traditional '#' also
        return !line.isEmpty() && !(line.startsWith("#") || line.startsWith(";"));
    }

    private List<Section> generateSections(Path directory, Path referenceDir, Predicate<String> pathFilter) {
        List<Section> sections = new ArrayList<>();

        Set<String> dirPaths = walkDirectory(directory, pathFilter);
        Set<String> refPaths = walkDirectory(referenceDir, pathFilter);

        Set<String> allPaths = new TreeSet<>();
        allPaths.addAll(dirPaths);
        allPaths.addAll(refPaths);

        allPaths.forEach(path -> {
            if (dirPaths.contains(path)) {
                Path currentFile = directory.resolve(path);
                Path referenceFile = refPaths.contains(path) ? referenceDir.resolve(path) : null;
                FileHandler fileHandler = retrieveFileHandler(currentFile);
                sections.add(fileHandler.diff(currentFile, path, referenceFile));
            } else {
                // file deleted
                sections.add(new Section(path, Section.Mode.DELETE, null, singletonList("")));
            }
        });

        return sections;
    }

    private Set<String> walkDirectory(Path dir, Predicate<String> pathFilter) {
        int pathIndex = dir.toString().length() + 1;
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk //
                    .filter(p -> Files.isRegularFile(p)) //
                    .map(p -> p.toString().substring(pathIndex).replace("\\", "/")) //
                    .filter(pathFilter) //
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeSections(List<Section> sections, Path outputFile) {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outputFile, INI_CHARSET))) {
            sections.forEach(section -> {
                String mode = "";
                switch (section.getMode()) {
                    case APPLY:
                        mode = " apply";
                        break;
                    case DELETE:
                        mode = " delete";
                        break;
                }
                String encoding = section.getEncoding();
                if (encoding != null) {
                    encoding = " @" + encoding.toLowerCase();
                } else {
                    encoding = "";
                }

                pw.printf("[%s%s%s]%n", section.getFilePath(), mode, encoding);
                section.getLines().forEach(pw::println);
                pw.println();
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Section> parseSections(Path iniFile) {
        List<Section> sections = new ArrayList<>();

        try (Stream<String> lines = Files.lines(iniFile, INI_CHARSET)) {
            AtomicReference<String> currentSectionRef = new AtomicReference<>();
            List<String> sectionLines = new ArrayList<>();

            lines.map(String::trim).forEach(line -> {
                final String currentSection = currentSectionRef.get();

                Matcher matcher = SECTION_MARKER.matcher(line);
                if (matcher.matches()) {
                    if (currentSection != null) {
                        sections.add(buildSection(currentSection, sectionLines));
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
                sections.add(buildSection(currentSection, sectionLines));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return sections;
    }

    private Section buildSection(String currentSection, List<String> sectionLines) {
        // trim section lines
        while (!sectionLines.isEmpty() && sectionLines.get(sectionLines.size() - 1).isEmpty()) {
            sectionLines.remove(sectionLines.size() - 1);
        }

        String[] elements = currentSection.split(" +");
        if (elements.length < 1 || elements.length > 2) {
            throw new IllegalArgumentException("Invalid INI section header : " + currentSection);
        }

        String fileName = elements[0];
        Section.Mode mode = Section.Mode.OVERWRITE;
        String encoding = null;
        if (elements.length > 1) {
            String element = elements[1];
            if ("apply".equalsIgnoreCase(element)) {
                mode = Section.Mode.APPLY;
            } else if ("delete".equalsIgnoreCase(element)) {
                mode = Section.Mode.DELETE;
            } else if (element.startsWith("@")) {
                encoding = element.substring(1);
            }
        }

        return new Section(fileName, mode, encoding, new ArrayList<>(sectionLines));
    }

    private void processSection(Transaction tx, Section section, Path target) {

        Path file = target.resolve(section.getFilePath());
        switch (section.getMode()) {
            case DELETE:
                handleDelete(tx, file, section.getLines());
                break;
            case APPLY:
                handleApply(tx, file, section.getLines());
                break;
            case OVERWRITE:
                handleOverride(tx, file, section.getLines(), section.getEncoding());
                break;
        }
    }

    private void handleDelete(Transaction tx, Path file, List<String> lines) {
        if (lines.stream().anyMatch(JConfigImpl::isContentLine)) {
            throw new IllegalStateException("Delete section contains content");
        }

        tx.deleteFile(file);
    }

    private void handleOverride(Transaction tx, Path file, List<String> lines, @Nullable String encoding) {
        Path outputFile = tx.getOutputFile(file);
        try {
            Files.createDirectories(outputFile.getParent());

            if ("base64".equals(encoding)) {
                Base64.Decoder decoder = Base64.getMimeDecoder();
                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
                    for (String line : lines) {
                        out.write(decoder.decode(line));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                Charset charset;
                if (encoding != null) {
                    charset = Charset.forName(encoding);
                } else {
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
                Path parent = tmpFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
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
