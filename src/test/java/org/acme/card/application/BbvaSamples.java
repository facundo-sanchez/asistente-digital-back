package org.acme.card.application;

import org.acme.card.application.model.ExtractedCard;
import org.acme.card.application.model.ExtractedStatement;
import org.acme.card.application.model.ExtractedTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lo que la IA lee de los ejemplos reales de BBVA (resumen en PDF y planilla de movimientos). */
public final class BbvaSamples {

    private BbvaSamples() {
    }

    /** PDF: con cierre, vencimiento, saldo anterior, pago y movimientos agrupados por titular. */
    public static ExtractedStatement pdf() {
        return new ExtractedStatement("BBVA", "4521", date(2026, 10, 29), date(2026, 11, 11), date(2026, 11, 26), date(2026, 12, 9),
                amount("269773.60"), BigDecimal.ZERO, amount("216050.90"), BigDecimal.ZERO, amount("58128.00"),
                List.of(),
                List.of(
                        tx(date(2026, 10, 5), "SU PAGO EN PESOS", null, null, null, "-269773.60", "PAYMENT", null),
                        tx(date(2026, 7, 9), "MERPAGO*LAPARFUMERIE C.04/12", "LAPARFUMERIE", 4, 12, "15466.66", "INSTALLMENT", null),
                        tx(date(2026, 10, 6), "APPYPF 30235 COMBUST", "APPYPF", null, null, "70000.07", "PURCHASE", null),
                        tx(date(2025, 5, 12), "MERPAGO*OVERTECH C.18/18", "OVERTECH", 18, 18, "18055.50", "INSTALLMENT", null),
                        tx(date(2025, 12, 26), "BRINGERI C.10/12", "BRINGERI", 10, 12, "31249.89", "INSTALLMENT", null),
                        tx(date(2026, 5, 10), "MERPAGO*RAMENS C.06/06", "RAMENS", 6, 6, "17736.95", "INSTALLMENT", null),
                        tx(date(2026, 5, 13), "MERPAGO*MERCADOLIBRE C.06/06", "MERCADOLIBRE", 6, 6, "60979.96", "INSTALLMENT", null),
                        tx(date(2026, 10, 29), "IMPUESTO DE SELLOS", null, null, null, "2561.87", "TAX", null)));
    }

    /** Planilla: sin cierre ni vencimiento, con totales por tarjeta (****0002 y ****8995). */
    public static ExtractedStatement excel() {
        return new ExtractedStatement("BBVA", null, null, null, null, null, null, null, amount("216050.90"), BigDecimal.ZERO, null,
                List.of(new ExtractedCard("0002", null, amount("128022.30"), BigDecimal.ZERO),
                        new ExtractedCard("8995", null, amount("88028.60"), BigDecimal.ZERO)),
                List.of(
                        tx(date(2026, 5, 13), "MERPAGO*MERCADOLIBRE", "MERCADOLIBRE", 6, 6, "60979.96", "INSTALLMENT", "0002"),
                        tx(date(2026, 5, 10), "MERPAGO*RAMENS", "RAMENS", 6, 6, "17736.95", "INSTALLMENT", "0002"),
                        tx(date(2025, 12, 26), "BRINGERI", "BRINGERI", 10, 12, "31249.89", "INSTALLMENT", "0002"),
                        tx(date(2025, 5, 12), "MERPAGO*OVERTECH", "OVERTECH", 18, 18, "18055.50", "INSTALLMENT", "0002"),
                        tx(date(2026, 10, 29), "IMPUESTO DE SELLOS", null, null, null, "2561.87", "TAX", "8995"),
                        tx(date(2026, 10, 6), "APPYPF 30235 COMBUST", "APPYPF", null, null, "70000.07", "PURCHASE", "8995"),
                        tx(date(2026, 7, 9), "MERPAGO*LAPARFUMERIE", "LAPARFUMERIE", 4, 12, "15466.66", "INSTALLMENT", "8995")));
    }

    private static ExtractedTransaction tx(LocalDate date, String description, String merchant, Integer number, Integer total,
                                           String amount, String type, String cardLastFour) {
        return new ExtractedTransaction(date, description, merchant, number, total, amount(amount), "ARS", type, cardLastFour, null);
    }

    private static LocalDate date(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
