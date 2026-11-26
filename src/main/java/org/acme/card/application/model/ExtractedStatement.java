package org.acme.card.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de tarjeta leído por la IA (esquema card_statement/1). Todo puede venir en null: el
 * Excel de BBVA, por ejemplo, no informa cierre ni vencimiento.
 */
public record ExtractedStatement(
        String bank,
        String accountLastFour,
        LocalDate closingDate,
        LocalDate dueDate,
        LocalDate nextClosingDate,
        LocalDate nextDueDate,
        BigDecimal previousBalance,
        BigDecimal previousBalanceUsd,
        BigDecimal totalAmount,
        BigDecimal totalAmountUsd,
        BigDecimal minimumPayment,
        List<ExtractedCard> cards,
        List<ExtractedTransaction> transactions
) {
    public static final String SCHEMA_VERSION = "card_statement/1";

    public List<ExtractedCard> cardsOrEmpty() {
        return cards == null ? List.of() : cards;
    }

    public List<ExtractedTransaction> transactionsOrEmpty() {
        return transactions == null ? List.of() : transactions;
    }
}
