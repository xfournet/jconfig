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
            writeSections(sections, tx.updateFile(diffFile));
            tx.commit();
        }
    }

    @Override
    public void merge(Path source) {
        if (Files.isDirectory(source)) {
            try (Stream<Path> walk = Files.walk(source)) {
                merge(walk.
                        map(path -> {
                            Path relativePath = source.relativize(path);
                            if (Files.isDirectory(path)) {
                                return newDirectoryEntry(relativePath);
                            } else {
                                return newRegularFileEntry(relativePath, () -> Files.newInputStream(path));
                            }
                        }).
                        filter(fileEntry -> m_pathFilter.test(fileEntry.path())));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            try (ZipFile zipFile = new ZipFile(source.toFile())) {
                Set<Path> directories = new HashSet<>();
                merge(zipFile.stream().
                        flatMap(zipEntry -> createZipFileAndParentDirectoryEntries(directories, zipFile, zipEntry).stream()).
                        filter(fileEntry -> m_pathFilter.test(fileEntry.path())));
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

                if (fileEntry.isDirectory()) {
                    tx.ensureDirectory(destinationFile);
                } else {
                    Path outputFile = tx.updateFile(destinationFile);
                    if (Files.exists(destinationFile)) {
                        try (InputStream update = fileEntry.open(); InputStream reference = Files.newInputStream(destinationFile);
                             OutputStream resultOutput = Files.newOutputStream(outputFile)) {
                            retrieveFileHandler(fileEntry.path()).merge(update, reference, resultOutput);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    } else {
                        try (InputStream update = fileEntry.open()) {
                            Files.copy(update, outputFile);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
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
            Path outputFile = tx.updateFile(resolvedDestinationFile);
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
            Path outputFile = tx.updateFile(resolvedFile);
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
            Path outputFile = tx.updateFile(resolvedFile);
            try (InputStream sourceInput = Files.newInputStream(resolvedFile); OutputStream resultOutput = Files.newOutputStream(outputFile)) {
                fileContentHandler.removeEntries(sourceInput, resultOutput, entries);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            tx.commit();
        }
    }

    @Override
    public void filter(Path file, UnaryOperator<String> expressionProcessor) {
        FileContentHandler fileContentHandler = retrieveFileHandler(file);
        Path resolvedFile = m_targetDir.resolve(file);

        try (Transaction tx = new Transaction()) {
            Path outputFile = tx.updateFile(resolvedFile);
            try (InputStream sourceInput = Files.newInputStream(resolvedFile); OutputStream resultOutput = Files.newOutputStream(outputFile)) {
                fileContentHandler.filter(sourceInput, resultOutput, expressionProcessor);
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

    /**
     * In a zip file you may or may not have entries for the directories. This method creates {@link FileEntry} instances for the file and for its parent
     * directories that were never seen before.
     *
     * @param directories List of all the directories previously seen
     * @param zipFile Current zip file
     * @param zipEntry Current zip entry
     * @return Entries for the parent directories never seen before and for the file itself
     */
    private List<FileEntry> createZipFileAndParentDirectoryEntries(Set<Path> directories, ZipFile zipFile, ZipEntry zipEntry) {
        List<FileEntry> result = new ArrayList<>();
        Path path = Paths.get(zipEntry.getName());

        // Append parent directories if not already defined
        for (int i = 1; i < path.getNameCount(); i++) {
            Path parent = path.subpath(0, i);
            if (directories.add(parent)) {
                result.add(newDirectoryEntry(parent));
            }
        }

        // Append zip entry
        // Always add it if it's a file, if it's a directory we check if we don't already have it
        if (!zipEntry.isDirectory()) {
            result.add(newRegularFileEntry(path, () -> zipFile.getInputStream(zipEntry)));
        } else if (directories.add(path)) {
            result.add(newDirectoryEntry(path));
        }

        return result;
    }

    private FileEntryImpl newRegularFileEntry(Path path, InputStreamSupplier inputStreamSupplier) {
        return new FileEntryImpl(path, false, inputStreamSupplier);
    }

    private FileEntryImpl newDirectoryEntry(Path path) {
        return new FileEntryImpl(path, true, () -> {
            throw new IOException("Cannot read a directory");
        });
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

        Diff diff = getDiff(currentSection, sectionLines, mode, encoding);
        return new Section(fileName, diff);
    }

    @Nullable
    private Diff getDiff(String currentSection, List<String> sectionLines, String mode, @Nullable String encoding) {
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
        return diff;
    }

    private void processSection(Transaction tx, Section section) {
        Diff diff = section.getDiff();
        Path targetPath = Paths.get(section.getPath());
        Path targetFile = m_targetDir.resolve(targetPath);
        if (diff != null) {
            FileContentHandler fileContentHandler = retrieveFileHandler(targetPath);

            Path outputFile = tx.updateFile(targetFile);
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
        Set<Path> dirPaths = listFiles(m_targetDir);
        Set<Path> refPaths = listFiles(referenceDir);

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

    private Set<Path> listFiles(Path dir) {
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

    private static final class Section {
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

    private interface InputStreamSupplier {
        InputStream get() throws IOException;
    }

    private static final class FileEntryImpl implements FileEntry {
        private final Path m_path;
        private final boolean m_directory;
        private final InputStreamSupplier m_inputStreamSupplier;

        private FileEntryImpl(Path path, boolean directory, InputStreamSupplier inputStreamSupplier) {
            m_path = path;
            m_directory = directory;
            m_inputStreamSupplier = inputStreamSupplier;
        }

        @Override
        public Path path() {
            return m_path;
        }

        @Override
        public boolean isDirectory() {
            return m_directory;
        }

        @Override
        public InputStream open() throws IOException {
            return m_inputStreamSupplier.get();
        }
    }
}
