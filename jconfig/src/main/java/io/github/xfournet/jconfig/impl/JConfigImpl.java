package io.github.xfournet.jconfig.impl;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;
import javax.annotation.*;
import io.github.xfournet.jconfig.Diff;
import io.github.xfournet.jconfig.FileContentHandler;
import io.github.xfournet.jconfig.FileEntry;
import io.github.xfournet.jconfig.JConfig;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.*;

public class JConfigImpl implements JConfig {
    private static final Charset DIFF_CHARSET = UTF_8;
    private static final Pattern SECTION_MARKER = Pattern.compile("^\\[(.+)]( +#.*)?$");

    private static final String SECTION_OVERWRITE = "overwrite";
    private static final String SECTION_MERGE = "merge";
    private static final String SECTION_DELETE = "delete";

    private final Path m_targetDir;
    private final Predicate<Path> m_pathFilter;
    private final Function<Path, FileContentHandler> m_fileHandlerResolver;

    public JConfigImpl(Path targetDir, Predicate<Path> pathFilter, Function<Path, FileContentHandler> fileHandlerResolver) {
        m_targetDir = targetDir;
        m_pathFilter = pathFilter;
        m_fileHandlerResolver = fileHandlerResolver;
    }

    @Override
    public Path targetDir() {
        return m_targetDir;
    }

    @Override
    public void apply(Path diffFile) {
        List<String> diffFileLines;
        try {
            diffFileLines = Files.readAllLines(diffFile, DIFF_CHARSET);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        List<Section> sections = parseSections(diffFileLines, diffFile);

        try (Transaction tx = new Transaction()) {
            sections.forEach(section -> processSection(tx, section));

            tx.commit();
        }
    }

    @Override
    public void diff(Path referenceDir, Path diffFile) {
        try (Transaction tx = new Transaction()) {
            List<Section> sections = generateSections(referenceDir);
            writeSections(sections, tx.getOutputFile(diffFile));

            tx.commit();
        }
    }

    @Override
    public void merge(Path source) {
        if (Files.isDirectory(source)) {
            merge(walkDirectory(source).stream().map(path -> new PathFileEntry(source.resolve(path), path)));
        } else {
            try (ZipFile zipFile = new ZipFile(source.toFile())) {
                Stream<? extends FileEntry> contentStream = zipFile.stream().
                        filter(e -> !e.isDirectory()).
                        map(e -> new ZipFileEntry(zipFile, e)).
                        filter(fe -> m_pathFilter.test(fe.path()));

                merge(contentStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void merge(Stream<? extends FileEntry> sourceFileEntries) {
        try (Transaction tx = new Transaction()) {
            sourceFileEntries.forEach(fileEntry -> {
                Path destinationFile = m_targetDir.resolve(fileEntry.path());
                Path outputFile = tx.getOutputFile(destinationFile);
                if (Files.exists(destinationFile)) {
                    try (InputStream source1Input = fileEntry.open(); InputStream source2Input = Files.newInputStream(destinationFile);
                         OutputStream resultOutput = Files.newOutputStream(outputFile)) {
                        retrieveFileHandler(fileEntry.path()).merge(source1Input, source2Input, resultOutput);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else {
                    try (InputStream source1Input = fileEntry.open()) {
                        Files.copy(source1Input, outputFile);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
            tx.commit();
        }
    }

    @Override
    public void merge(Path destinationFile, Path sourceFile) {
        FileContentHandler fileContentHandler = retrieveFileHandler(destinationFile);
        Path resolvedDestinationFile = m_targetDir.resolve(destinationFile);

        try (Transaction tx = new Transaction()) {
            Path outputFile = tx.getOutputFile(resolvedDestinationFile);
            try (InputStream source1Input = Files.newInputStream(sourceFile); InputStream source2Input = Files.newInputStream(resolvedDestinationFile);
                 OutputStream resultOutput = Files.newOutputStream(outputFile)) {
                fileContentHandler.merge(source1Input, source2Input, resultOutput);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            tx.commit();
        }
    }

    @Override
    public void setEntries(Path file, List<String> entries) {
        FileContentHandler fileContentHandler = retrieveFileHandler(file);
        Path resolvedFile = m_targetDir.resolve(file);

        try (Transaction tx = new Transaction()) {
            Path outputFile = tx.getOutputFile(resolvedFile);
            try (InputStream sourceInput = Files.newInputStream(resolvedFile); OutputStream resultOutput = Files.newOutputStream(outputFile)) {
                fileContentHandler.setEntries(sourceInput, resultOutput, entries);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            tx.commit();
        }
    }

    @Override
    public void removeEntries(Path file, List<String> entries) {
        FileContentHandler fileContentHandler = retrieveFileHandler(file);
        Path resolvedFile = m_targetDir.resolve(file);

        try (Transaction tx = new Transaction()) {
            Path outputFile = tx.getOutputFile(resolvedFile);
            try (InputStream sourceInput = Files.newInputStream(resolvedFile); OutputStream resultOutput = Files.newOutputStream(outputFile)) {
                fileContentHandler.removeEntries(sourceInput, resultOutput, entries);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            tx.commit();
        }
    }

    @Override
    public void filter(Path file, Function<String, String> variableResolver) {
        FileContentHandler fileContentHandler = retrieveFileHandler(file);
        Path resolvedFile = m_targetDir.resolve(file);

        try (Transaction tx = new Transaction()) {
            Path outputFile = tx.getOutputFile(resolvedFile);
            try (InputStream sourceInput = Files.newInputStream(resolvedFile); OutputStream resultOutput = Files.newOutputStream(outputFile)) {
                fileContentHandler.filter(sourceInput, resultOutput, variableResolver);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            tx.commit();
        }
    }

    private FileContentHandler retrieveFileHandler(Path path) {
        return Optional.of(path).
                map(m_fileHandlerResolver).
                orElseThrow(() -> new IllegalArgumentException("No handler was found for file: " + path));
    }

    //region apply related code
    private List<Section> parseSections(List<String> diffFileContent, Path diffFile) {
        List<Section> sections = new ArrayList<>();

        String currentSection = null;
        List<String> sectionLines = new ArrayList<>();
        for (String line : diffFileContent) {
            Matcher matcher = SECTION_MARKER.matcher(line);
            if (matcher.matches()) {
                if (currentSection != null) {
                    sections.add(buildSection(currentSection, sectionLines));
                }
                currentSection = matcher.group(1);
                sectionLines = new ArrayList<>();
            } else {
                if (currentSection != null) {
                    sectionLines.add(line);
                } else if (isContentLine(line)) {
                    throw new IllegalArgumentException("Content outside section in " + diffFile);
                }
            }
        }

        if (currentSection != null) {
            sections.add(buildSection(currentSection, sectionLines));
        }

        Set<String> duplicates = new HashSet<>();
        sections.stream().map(Section::getPath).forEach(path -> {
            if (!duplicates.add(path)) {
                throw new IllegalArgumentException("Duplicate section for path: " + path);
            }
        });

        return sections;
    }

    private static boolean isContentLine(String line) {
        // traditional .ini file comment mark is ';' , add support of traditional '#' also
        return !line.isEmpty() && !(line.startsWith("#") || line.startsWith(";"));
    }

    private Section buildSection(String currentSection, List<String> sectionLines) {
        // trim section lines
        while (!sectionLines.isEmpty() && sectionLines.get(sectionLines.size() - 1).isEmpty()) {
            sectionLines.remove(sectionLines.size() - 1);
        }

        String[] elements = currentSection.split(" +");
        if (!(elements.length == 2 || elements.length == 3)) {
            throw new IllegalArgumentException("Invalid section header : " + currentSection);
        }

        String fileName = elements[0];
        String mode = elements[1];
        String encoding = elements.length == 3 ? elements[2].substring(1) : null;

        Diff diff;
        switch (mode) {
            case SECTION_OVERWRITE:
                diff = new Diff(true, encoding, sectionLines);
                break;

            case SECTION_MERGE:
                diff = new Diff(false, null, sectionLines);
                break;

            case SECTION_DELETE:
                if (sectionLines.stream().anyMatch(JConfigImpl::isContentLine)) {
                    throw new IllegalArgumentException("Delete section contains content");
                }

                diff = null;
                break;

            default:
                throw new IllegalArgumentException("Invalid section header : " + currentSection);
        }

        return new Section(fileName, diff);
    }

    private void processSection(Transaction tx, Section section) {
        Diff diff = section.getDiff();
        Path targetPath = Paths.get(section.getPath());
        Path targetFile = m_targetDir.resolve(targetPath);
        if (diff != null) {
            FileContentHandler fileContentHandler = retrieveFileHandler(targetPath);

            Path outputFile = tx.getOutputFile(targetFile);
            try (InputStream sourceInput = Files.exists(targetFile) ? Files.newInputStream(targetFile) : null;
                 OutputStream resultOutput = Files.newOutputStream(outputFile)) {
                fileContentHandler.apply(sourceInput, resultOutput, diff);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            tx.deleteFile(targetFile);
        }
    }
    //endregion

    //region diff related code
    private List<Section> generateSections(Path referenceDir) {
        Set<Path> dirPaths = walkDirectory(m_targetDir);
        Set<Path> refPaths = walkDirectory(referenceDir);

        Set<Path> allPaths = new TreeSet<>();
        allPaths.addAll(dirPaths);
        allPaths.addAll(refPaths);

        return allPaths.stream().
                map(path -> {
                    Diff diff;
                    if (dirPaths.contains(path)) {
                        Path currentFile = m_targetDir.resolve(path);
                        Path referenceFile = refPaths.contains(path) ? referenceDir.resolve(path) : null;
                        FileContentHandler fileContentHandler = retrieveFileHandler(path);
                        try (InputStream source = Files.newInputStream(currentFile);
                             InputStream referenceSource = referenceFile != null ? Files.newInputStream(referenceFile) : null) {
                            diff = fileContentHandler.diff(source, referenceSource);
                            if (diff == null) {
                                return null; // no diff, will be filtered in the stream filter below
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    } else {
                        // file deleted
                        diff = null;
                    }
                    String filePath = path.toString().replace("\\", "/");
                    return new Section(filePath, diff);
                }).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    private Set<Path> walkDirectory(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.
                    filter(Files::isRegularFile).
                    map(dir::relativize).
                    filter(m_pathFilter).
                    collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeSections(List<Section> sections, Path outputFile) {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outputFile, DIFF_CHARSET))) {
            sections.forEach(section -> {
                Diff diff = section.getDiff();

                String mode;
                String encoding;
                if (diff != null) {
                    if (diff.isOverwrite()) {
                        mode = SECTION_OVERWRITE;
                    } else {
                        mode = SECTION_MERGE;
                    }
                    encoding = diff.getEncoding();
                    if (encoding != null) {
                        encoding = " @" + encoding.toLowerCase();
                    } else {
                        encoding = "";
                    }
                } else {
                    mode = SECTION_DELETE;
                    encoding = "";
                }

                pw.printf("[%s %s%s]%n", section.getPath(), mode, encoding);
                if (diff != null) {
                    diff.getLines().forEach(pw::println);
                }
                pw.println();
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    //endregion

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

    private static class Section {
        private final String m_path;
        @Nullable
        private final Diff m_diff;

        Section(String path, @Nullable Diff diff) {
            m_path = path;
            m_diff = diff;
        }

        String getPath() {
            return m_path;
        }

        /**
         * @return {@code null} if the file has to be deleted else the {@link Diff} object
         */
        @Nullable
        Diff getDiff() {
            return m_diff;
        }
    }

    private static class PathFileEntry implements FileEntry {
        private final Path m_realPath;
        private final Path m_relativePath;

        PathFileEntry(Path realPath, Path relativePath) {
            m_realPath = realPath;
            m_relativePath = relativePath;
        }

        @Override
        public Path path() {
            return m_relativePath;
        }

        @Override
        public InputStream open() throws IOException {
            return Files.newInputStream(m_realPath);
        }
    }

    private static class ZipFileEntry implements FileEntry {
        private final Path m_path;
        private final ZipFile m_zipFile;
        private final ZipEntry m_zipEntry;

        ZipFileEntry(ZipFile zipFile, ZipEntry zipEntry) {
            m_path = Paths.get(zipEntry.getName());
            m_zipFile = zipFile;
            m_zipEntry = zipEntry;
        }

        @Override
        public Path path() {
            return m_path;
        }

        @Override
        public InputStream open() throws IOException {
            return m_zipFile.getInputStream(m_zipEntry);
        }
    }
}
