package org.acme.document.infrastructure.ai;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Una planilla como la de "Movimientos del período" de BBVA, convertida a texto. */
class SpreadsheetTextTest {

    @Test
    void unaLineaPorFilaSinFilasNiCeldasVaciasAlFinal() throws IOException {
        byte[] file;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Mov_Periodo");
            row(sheet, 0, "Nro. Tarjeta", "Fecha", "Establecimiento", "Cuota", "Importe en $", "Importe en U$S");
            row(sheet, 1, "", "26/12/2025", "BRINGERI", "10/12", "31.249,89", "");
            sheet.createRow(2); // fila vacía
            row(sheet, 3, "Total Tarjeta Nro ****0002", "", "", "", "128.022,30", "0,00");
            workbook.write(out);
            file = out.toByteArray();
        }

        assertEquals("""
                ## Hoja: Mov_Periodo
                Nro. Tarjeta | Fecha | Establecimiento | Cuota | Importe en $ | Importe en U$S
                 | 26/12/2025 | BRINGERI | 10/12 | 31.249,89
                Total Tarjeta Nro ****0002 |  |  |  | 128.022,30 | 0,00
                """, SpreadsheetText.toText(new ByteArrayInputStream(file)));
    }

    private static void row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
