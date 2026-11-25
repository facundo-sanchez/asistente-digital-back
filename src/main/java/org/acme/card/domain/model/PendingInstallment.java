package org.acme.card.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Compra en cuotas con cuotas restantes (vista v_card_pending_installments).
 *
 * @param id                       id del último movimiento registrado de la compra
 * @param estimatedLastClosingDate cierre estimado del resumen que incluirá la última cuota
 */
public record PendingInstallment(
        UUID id,
        UUID cardId,
        UUID purchaseGroupId,
        LocalDate purchaseDate,
        String merchant,
        SpendingCategory category,
        String currency,
        BigDecimal installmentAmount,
        Integer installmentNumber,
        Integer installmentTotal,
        Integer remainingInstallments,
        BigDecimal remainingAmount,
        BigDecimal originalAmount,
        LocalDate lastClosingDate,
        LocalDate estimatedLastClosingDate
) {}
