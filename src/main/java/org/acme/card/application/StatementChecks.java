package org.acme.card.application;

import org.acme.card.application.model.ExtractedCard;
import org.acme.card.application.model.ExtractedStatement;
import org.acme.card.application.model.ExtractedTransaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Controles sobre lo que leyó la IA, para no confiar a ciegas (CU-016 paso 7): datos que faltan,
 * cuadre de los movimientos con los totales (por moneda y por tarjeta) y cuotas válidas.
 */
public final class StatementChecks {

    /** Diferencias menores a un centavo son redondeo. */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final String USD = "USD";

    private StatementChecks() {
    }

    public static List<String> warnings(ExtractedStatement statement) {
        List<String> warnings = new ArrayList<>();
        if (statement.dueDate() == null) {
            warnings.add("El archivo no informa el vencimiento: completalo antes de confirmar.");
        }
        balance(statement, false, statement.previousBalance(), statement.totalAmount()).ifPresent(warnings::add);
        balance(statement, true, statement.previousBalanceUsd(), statement.totalAmountUsd()).ifPresent(warnings::add);
        for (ExtractedCard card : statement.cardsOrEmpty()) {
            cardTotal(statement, card).ifPresent(warnings::add);
        }
        long badInstallments = statement.transactionsOrEmpty().stream().filter(t -> !validInstallment(t)).count();
        if (badInstallments > 0) {
            warnings.add(badInstallments + " movimiento(s) tienen una cuota que no tiene sentido (ej. 13 de 12): revisalos.");
        }
        return warnings;
    }

    /** Suma de los movimientos de esa moneda (con su signo). */
    public static BigDecimal sum(ExtractedStatement statement, boolean usd) {
        return statement.transactionsOrEmpty().stream()
                .filter(t -> isUsd(t) == usd)
                .map(ExtractedTransaction::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static boolean validInstallment(ExtractedTransaction t) {
        if (t.installmentNumber() == null && t.installmentTotal() == null) {
            return true;
        }
        return t.installmentNumber() != null && t.installmentTotal() != null
                && t.installmentTotal() >= 1 && t.installmentTotal() <= 99
                && t.installmentNumber() >= 1 && t.installmentNumber() <= t.installmentTotal();
    }

    /**
     * Con saldo anterior (resumen en PDF): anterior + movimientos = total. Sin él (planilla de
     * movimientos): movimientos = total.
     */
    private static Optional<String> balance(ExtractedStatement statement, boolean usd, BigDecimal previous, BigDecimal total) {
        BigDecimal movements = sum(statement, usd);
        if (total == null || (usd && total.signum() == 0 && movements.signum() == 0)) {
            return Optional.empty();
        }
        BigDecimal expected = (previous == null ? BigDecimal.ZERO : previous).add(movements);
        BigDecimal difference = total.subtract(expected);
        if (difference.abs().compareTo(TOLERANCE) <= 0) {
            return Optional.empty();
        }
        String currency = usd ? "dólares" : "pesos";
        return Optional.of("Los movimientos en " + currency + " no cuadran con el total del resumen ("
                + money(total) + "): diferencia de " + money(difference) + ". Puede faltar o sobrar algún movimiento.");
    }

    private static Optional<String> cardTotal(ExtractedStatement statement, ExtractedCard card) {
        if (card.lastFour() == null || card.total() == null) {
            return Optional.empty();
        }
        BigDecimal movements = statement.transactionsOrEmpty().stream()
                .filter(t -> card.lastFour().equals(t.cardLastFour()) && !isUsd(t) && t.amount() != null)
                .map(ExtractedTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (card.total().subtract(movements).abs().compareTo(TOLERANCE) <= 0) {
            return Optional.empty();
        }
        return Optional.of("Los movimientos de la tarjeta terminada en " + card.lastFour() + " suman "
                + money(movements) + " y su total es " + money(card.total()) + ".");
    }

    private static boolean isUsd(ExtractedTransaction t) {
        return USD.equalsIgnoreCase(t.currency());
    }

    private static String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "$ " + format.format(amount);
    }
}
