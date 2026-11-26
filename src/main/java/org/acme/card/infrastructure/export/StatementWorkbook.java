package org.acme.card.infrastructure.export;

import org.acme.card.application.model.CardStatementView;
import org.acme.card.domain.model.CardStatement;
import org.acme.card.domain.model.CardTransaction;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** El resumen en Excel: una hoja con los datos y totales y otra con los movimientos (fecha, comercio, rubro, cuota, importe). */
public final class StatementWorkbook {

    private StatementWorkbook() {
    }

    public static byte[] build(CardStatementView view, List<CardTransaction> transactions) {
        CardStatement s = view.statement();
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle date = book.createCellStyle();
            date.setDataFormat(book.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle money = book.createCellStyle();
            money.setDataFormat(book.getCreationHelper().createDataFormat().getFormat("#,##0.00"));

            Sheet summary = book.createSheet("Resumen");
            int r = 0;
            r = text(summary, r, "Tarjeta", view.cardName());
            r = day(summary, r, "Cierre", s.getClosingDate(), date);
            r = day(summary, r, "Vencimiento", s.getDueDate(), date);
            r = day(summary, r, "Próximo cierre", s.getNextClosingDate(), date);
            r = day(summary, r, "Próximo vencimiento", s.getNextDueDate(), date);
            r = amount(summary, r, "Total en pesos", s.getTotalAmount(), money);
            r = amount(summary, r, "Total en dólares", s.getTotalAmountUsd(), money);
            amount(summary, r, "Pago mínimo", s.getMinimumPayment(), money);
            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);

            Sheet movements = book.createSheet("Movimientos");
            Row header = movements.createRow(0);
            String[] titles = {"Fecha", "Comercio", "Descripción", "Rubro", "Cuota", "Tipo", "Importe", "Moneda"};
            for (int i = 0; i < titles.length; i++) header.createCell(i).setCellValue(titles[i]);
            int i = 1;
            for (CardTransaction t : transactions) {
                Row row = movements.createRow(i++);
                row.createCell(0).setCellValue(t.getTransactionDate());
                row.getCell(0).setCellStyle(date);
                row.createCell(1).setCellValue(t.getMerchantName() == null ? "" : t.getMerchantName());
                row.createCell(2).setCellValue(t.getDescription());
                row.createCell(3).setCellValue(t.getCategory().name());
                row.createCell(4).setCellValue(t.getInstallmentTotal() == null ? "" : t.getInstallmentNumber() + "/" + t.getInstallmentTotal());
                row.createCell(5).setCellValue(t.getTransactionType().name());
                row.createCell(6).setCellValue(t.getAmount().doubleValue());
                row.getCell(6).setCellStyle(money);
                row.createCell(7).setCellValue(t.getCurrency());
            }
            for (int c = 0; c < titles.length; c++) movements.autoSizeColumn(c);
            book.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int text(Sheet sheet, int r, String label, String value) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value == null ? "" : value);
        return r + 1;
    }

    private static int day(Sheet sheet, int r, String label, LocalDate value, CellStyle style) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(label);
        if (value != null) {
            row.createCell(1).setCellValue(value);
            row.getCell(1).setCellStyle(style);
        }
        return r + 1;
    }

    private static int amount(Sheet sheet, int r, String label, BigDecimal value, CellStyle style) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(label);
        if (value != null) {
            row.createCell(1).setCellValue(value.doubleValue());
            row.getCell(1).setCellStyle(style);
        }
        return r + 1;
    }
}
