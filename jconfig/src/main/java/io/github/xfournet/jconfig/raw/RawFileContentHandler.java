package io.github.xfournet.jconfig.raw;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.*;
import javax.annotation.*;
import io.github.xfournet.jconfig.Diff;
import io.github.xfournet.jconfig.FileContentHandler;

import static java.nio.charset.StandardCharsets.*;

public class RawFileContentHandler implements FileContentHandler {

    @Override
    public void apply(@Nullable InputStream source, OutputStream result, Diff diff) throws IOException {
        if (!diff.isOverwrite()) {
            throw new UnsupportedOperationException("Diff apply mode is not supported by " + RawFileContentHandler.class.getSimpleName());
        }

        String encoding = diff.getEncoding();
        if ("base64".equals(encoding)) {
            Base64.Decoder decoder = Base64.getMimeDecoder();
            for (String line : diff.getLines()) {
                result.write(decoder.decode(line));
            }
        } else {
            Charset charset = encoding != null ? Charset.forName(encoding) : UTF_8;
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(result, charset))) {
                for (String line : diff.getLines()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
    }

    @Override
    public Diff diff(InputStream source, @Nullable InputStream referenceSource) throws IOException {
        List<String> lines;
        String encoding = null;

        byte[] sourceContent = readFully(source);

        if (isTextFile(sourceContent)) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(sourceContent), UTF_8))) {
                lines = bufferedReader.lines().collect(Collectors.toList());
            }
        } else {
            encoding = "base64";

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Base64.Encoder mimeEncoder = Base64.getMimeEncoder();
            try (OutputStream out = mimeEncoder.wrap(bos)) {
                out.write(sourceContent);
            }

            byte[] base64Content = bos.toByteArray();
            String base64 = new String(base64Content, US_ASCII);
            lines = new ArrayList<>();
            Collections.addAll(lines, base64.split("[\n\r]+"));
        }

        return new Diff(true, encoding, lines);
    }

    @Override
    public void merge(InputStream contentToMerge, InputStream sourceToUpdate, OutputStream result) throws IOException {
        // merge = overwrite, sourceToUpdate is ignored
        copy(contentToMerge, result);
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];

        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copy(in, bos);
        return bos.toByteArray();
    }

    private static boolean isTextFile(byte[] content) {
        for (byte aByte : content) {
            int b = aByte & 0xFF;

            if ((b < 32 || b > 127) && (b != '\n' && b != '\r' && b != '\f' && b != '\t' && b != '\b')) {
                return false;
            }
        }

        return true;
    }
}
