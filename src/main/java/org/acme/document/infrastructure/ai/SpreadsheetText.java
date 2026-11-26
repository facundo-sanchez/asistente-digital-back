package org.acme.document.infrastructure.ai;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Convierte cualquier planilla (XLSX o XLS, de cualquier banco) en una tabla de texto que la IA
 * puede leer: una sección por hoja y una línea por fila con las celdas separadas por " | ".
 * Las celdas se muestran como en Excel (fechas e importes con su formato); las filas vacías no van.
 */
public final class SpreadsheetText {

    /** Tope para no mandar planillas enormes a la IA. */
    static final int MAX_ROWS = 2000;

    private SpreadsheetText() {
    }

    public static String toText(InputStream in) throws IOException {
        // es-AR: los importes salen como en el banco (1.234,56)
        DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("es-AR"));
        StringBuilder out = new StringBuilder();
        int rows = 0;
        try (Workbook workbook = WorkbookFactory.create(in)) {
            for (Sheet sheet : workbook) {
                out.append("## Hoja: ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    String line = line(row, formatter);
                    if (!line.isEmpty() && rows++ < MAX_ROWS) {
                        out.append(line).append('\n');
                    }
                }
            }
        }
        return out.toString();
    }

    private static String line(Row row, DataFormatter formatter) {
        List<String> cells = new ArrayList<>();
        int last = 0;
        for (int i = 0; i < Math.max(row.getLastCellNum(), 0); i++) {
            Cell cell = row.getCell(i);
            String value = cell == null ? "" : formatter.formatCellValue(cell).replace('\n', ' ').strip();
            cells.add(value);
            if (!value.isEmpty()) {
                last = cells.size();
            }
        }
        // Sin las celdas vacías del final; una fila sin nada queda vacía
        return last == 0 ? "" : String.join(" | ", cells.subList(0, last));
    }
}
