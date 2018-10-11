package io.github.xfournet.jconfig.raw;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.*;
import io.github.xfournet.jconfig.FileHandler;
import io.github.xfournet.jconfig.Section;

import static io.github.xfournet.jconfig.Section.Mode.OVERWRITE;

public class RawFileHandler implements FileHandler {
    @Override
    public boolean canHandle(Path file) {
        return true;
    }

    @Override
    public Section diff(Path file, String fileName, @Nullable Path referenceFile) {
        List<String> lines;
        String encoding = null;
        if (isTextFile(file)) {
            try {
                lines = Files.readAllLines(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            encoding = "base64";

            // TODO ugly code for base64 encoding...
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer, 0, buffer.length)) != -1) {
                    bos.write(buffer, 0, read);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            String base64 = Base64.getMimeEncoder().encodeToString(bos.toByteArray());
            lines = new ArrayList<>();
            Collections.addAll(lines, base64.split("[\n\r]+"));
        }

        return new Section(fileName, OVERWRITE, encoding, lines);
    }

    @Override
    public void normalize(Path file, Path destination) {
        copy(file, destination);
    }

    private boolean isTextFile(Path file) {
        // could be optimized ?
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            int read;
            while ((read = in.read()) != -1) {
                if (read < 32 || read > 127) {
                    if (read != 10 && read != 13 && read != 9) {
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return true;
    }

    private void copy(Path from, Path to) {
        try {
            Files.createDirectories(to.getParent());
            Files.copy(from, to);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
