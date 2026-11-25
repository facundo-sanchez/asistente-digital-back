package org.acme.document.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * Tipo real de un archivo según sus primeros bytes (no según la extensión ni lo que
 * diga el navegador). Solo los tipos admitidos: PDF, JPG, PNG y WEBP.
 */
public final class FileType {

    private FileType() {
    }

    /** MIME del archivo, o vacío si no es de un tipo admitido. */
    public static Optional<String> detect(Path file) throws IOException {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = Files.newInputStream(file)) {
            read = in.readNBytes(head, 0, head.length);
        }
        return detect(Arrays.copyOf(head, read));
    }

    static Optional<String> detect(byte[] head) {
        if (startsWith(head, "%PDF".getBytes())) {
            return Optional.of("application/pdf");
        }
        if (startsWith(head, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return Optional.of("image/jpeg");
        }
        if (startsWith(head, new byte[]{(byte) 0x89, 'P', 'N', 'G'})) {
            return Optional.of("image/png");
        }
        // WEBP: "RIFF" + 4 bytes de tamaño + "WEBP"
        if (head.length >= 12 && startsWith(head, "RIFF".getBytes())
                && Arrays.equals(Arrays.copyOfRange(head, 8, 12), "WEBP".getBytes())) {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length && Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
    }
}
