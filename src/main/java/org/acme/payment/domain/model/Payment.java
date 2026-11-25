package org.acme.payment.domain.model;

import lombok.*;
import org.acme.domain.exceptions.ServiceException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pago de una obligación (CU-006). Admite pagos parciales.
 * Los pagos no se borran: se anulan para conservar el historial (RN-008).
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private UUID id;
    private UUID userId;
    private UUID obligationId;
    private BigDecimal amount;
    private String currency;
    private LocalDate paymentDate;
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.OTHER;
    private UUID receiptDocumentId;
    private String notes;
    private Instant voidedAt;
    private String voidReason;
    /** Pago anterior que este reemplaza (corrección). */
    private UUID replacesPaymentId;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isVoided() {
        return voidedAt != null;
    }

    public void voidPayment(String reason, Instant now) {
        if (voidedAt != null) {
            throw new ServiceException("El pago ya está anulado");
        }
        if (reason == null || reason.isBlank()) {
            throw new ServiceException("El motivo de anulación es obligatorio");
        }
        voidReason = reason;
        voidedAt = now;
    }
}
