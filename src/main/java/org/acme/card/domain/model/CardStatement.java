package org.acme.card.domain.model;

import lombok.*;
import org.acme.domain.exceptions.ServiceException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Resumen mensual de una tarjeta (CU-015). Al confirmarse genera una obligación
 * de pago por cada moneda con saldo.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CardStatement {
    private UUID id;
    private UUID userId;
    private UUID cardId;
    private UUID documentId;
    private UUID extractionResultId;
    /** Obligación de pago en moneda local. */
    private UUID obligationId;
    /** Obligación de pago en dólares. */
    private UUID obligationUsdId;
    private LocalDate periodStart;
    /** Puede faltar si el resumen se detectó por email sin PDF (ej. BBVA). */
    private LocalDate closingDate;
    private LocalDate dueDate;
    private LocalDate nextClosingDate;
    private LocalDate nextDueDate;
    @Builder.Default
    private String currency = "ARS";
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalAmountUsd = BigDecimal.ZERO;
    private BigDecimal minimumPayment;
    @Builder.Default
    private CardStatementStatus status = CardStatementStatus.PENDING_VALIDATION;
    @Builder.Default
    private CardStatementSource source = CardStatementSource.MANUAL;
    /** Id del email de origen (idempotencia). Uso interno. */
    private String sourceMessageId;
    private Instant confirmedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public void confirm(Instant now) {
        if (status == CardStatementStatus.REJECTED) {
            throw new ServiceException("No se puede confirmar un resumen rechazado");
        }
        status = CardStatementStatus.CONFIRMED;
        confirmedAt = now;
    }

    /** Rechazar una propuesta o anular un resumen confirmado. Queda en el historial. */
    public void reject() {
        if (status == CardStatementStatus.REJECTED) {
            throw new ServiceException("El resumen ya está anulado");
        }
        status = CardStatementStatus.REJECTED;
    }

    public boolean hasLocalBalance() {
        return totalAmount != null && totalAmount.signum() > 0;
    }

    public boolean hasUsdBalance() {
        return totalAmountUsd != null && totalAmountUsd.signum() > 0;
    }
}
