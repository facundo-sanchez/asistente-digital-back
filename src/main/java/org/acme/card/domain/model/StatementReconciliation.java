package org.acme.card.domain.model;

import java.math.BigDecimal;

/**
 * Conciliación de un resumen en una moneda: total informado vs. suma de consumos (CU-016, paso 7).
 *
 * @param difference statementTotal - transactionsTotal (0 = conciliado)
 */
public record StatementReconciliation(
        String currency,
        BigDecimal statementTotal,
        BigDecimal transactionsTotal,
        BigDecimal difference
) {
    public boolean isBalanced() {
        return difference.signum() == 0;
    }
}
