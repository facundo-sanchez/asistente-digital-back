package org.acme.document.application;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @TempDir
    Path tmp;

    @Test
    void reconoceExcelPorSuContenido() throws IOException {
        assertEquals(Optional.of(FileType.XLSX), FileType.detect(zip("resumen.xlsx", "xl/workbook.xml")));
        assertEquals(Optional.empty(), FileType.detect(zip("carta.docx", "word/document.xml")), "otro ZIP no es Excel");
    }

    @Test
    void unXlsSoloCuentaSiSeAbreComoPlanilla() throws IOException {
        Path xls = tmp.resolve("resumen.xls");
        try (HSSFWorkbook workbook = new HSSFWorkbook(); var out = Files.newOutputStream(xls)) {
            workbook.createSheet("Movimientos").createRow(0).createCell(0).setCellValue("BRINGERI");
            workbook.write(out);
        }
        assertEquals(Optional.of(FileType.XLS), FileType.detect(xls));

        // Cabecera OLE (la de .doc y .msg) sin una planilla adentro
        Path fake = Files.write(tmp.resolve("carta.doc"), new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0, 0});
        assertEquals(Optional.empty(), FileType.detect(fake));
    }

    private Path zip(String name, String entry) throws IOException {
        Path file = tmp.resolve(name);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
            out.putNextEntry(new ZipEntry(entry));
            out.write("<x/>".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return file;
    }
}
