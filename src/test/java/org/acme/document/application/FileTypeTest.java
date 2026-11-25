package org.acme.document.application;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** El tipo se reconoce por los primeros bytes. */
class FileTypeTest {

    @Test
    void reconoceLosTiposAdmitidos() {
        assertEquals(Optional.of("application/pdf"), FileType.detect("%PDF-1.4".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(Optional.of("image/jpeg"), FileType.detect(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}));
        assertEquals(Optional.of("image/png"), FileType.detect(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D}));
        assertEquals(Optional.of("image/webp"), FileType.detect("RIFF0000WEBPVP8".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void rechazaElResto() {
        assertEquals(Optional.empty(), FileType.detect("MZ".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(Optional.empty(), FileType.detect("RIFF0000WAVE".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(Optional.empty(), FileType.detect(new byte[0]));
    }
}
