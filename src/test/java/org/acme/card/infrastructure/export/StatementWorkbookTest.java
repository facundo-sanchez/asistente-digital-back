package org.acme.card.infrastructure.export;

import org.acme.card.application.model.CardStatementView;
import org.acme.card.domain.model.CardStatement;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.domain.model.CardTransactionType;
import org.acme.card.domain.model.SpendingCategory;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementWorkbookTest {

    @Test
    void tieneLosTotalesYUnaFilaPorMovimiento() throws Exception {
        CardStatement statement = CardStatement.builder().dueDate(LocalDate.of(2026, 11, 11)).totalAmount(new BigDecimal("216050.90"))
                .totalAmountUsd(BigDecimal.ZERO).build();
        CardTransaction cuota = CardTransaction.builder().transactionDate(LocalDate.of(2026, 1, 5)).merchantName("BRINGERI").description("BRINGERI C.10/12")
                .category(SpendingCategory.CLOTHING).transactionType(CardTransactionType.INSTALLMENT).amount(new BigDecimal("15000"))
                .currency("ARS").installmentNumber(10).installmentTotal(12).build();

        byte[] file = StatementWorkbook.build(new CardStatementView(statement, "Visa BBVA", null, null), List.of(cuota));

        try (Workbook book = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertEquals("Visa BBVA", book.getSheet("Resumen").getRow(0).getCell(1).getStringCellValue());
            Sheet movements = book.getSheet("Movimientos");
            assertEquals(1, movements.getLastRowNum());
            assertEquals("10/12", movements.getRow(1).getCell(4).getStringCellValue());
            assertEquals(15000.0, movements.getRow(1).getCell(6).getNumericCellValue());
        }
    }
}
