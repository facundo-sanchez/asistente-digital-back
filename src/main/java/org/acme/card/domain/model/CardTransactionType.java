package org.acme.card.domain.model;

/**
 * Tipo de movimiento del resumen.
 * REFUND y PAYMENT siempre tienen importe negativo. PAYMENT (pago del resumen
 * anterior) no cuenta como gasto en el informe.
 */
public enum CardTransactionType {
    PURCHASE, INSTALLMENT, REFUND, FEE, TAX, INTEREST, PAYMENT, ADJUSTMENT
}
