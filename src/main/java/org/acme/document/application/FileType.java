package org.acme.document.application;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipFile;

/**
 * Tipo real de un archivo según sus primeros bytes (no según la extensión ni lo que
 * diga el navegador). Solo los tipos admitidos: PDF, JPG, PNG, WEBP y Excel (XLSX, XLS).
 */
public final class FileType {

    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String XLS = "application/vnd.ms-excel";

    private static final byte[] ZIP = {'P', 'K', 3, 4};
    private static final byte[] OLE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};

    private FileType() {
    }

    /** MIME del archivo, o vacío si no es de un tipo admitido. */
    public static Optional<String> detect(Path file) throws IOException {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = Files.newInputStream(file)) {
            read = in.readNBytes(head, 0, head.length);
        }
        byte[] start = Arrays.copyOf(head, read);
        // XLSX es un ZIP: solo cuenta si adentro está el libro de Excel (un .docx u otro ZIP no pasa)
        if (startsWith(start, ZIP)) {
            return isWorkbook(file) ? Optional.of(XLSX) : Optional.empty();
        }
        // XLS usa la cabecera OLE, igual que .doc y .msg: solo cuenta si se abre como planilla
        if (startsWith(start, OLE)) {
            return opensAsSpreadsheet(file) ? Optional.of(XLS) : Optional.empty();
        }
        return detect(start);
    }

    private static boolean opensAsSpreadsheet(Path file) {
        try (InputStream in = Files.newInputStream(file); Workbook ignored = WorkbookFactory.create(in)) {
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static boolean isWorkbook(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            return zip.getEntry("xl/workbook.xml") != null;
        } catch (IOException e) {
            return false;
        }
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
