package org.acme.obligation.domain.model;

import lombok.*;
import org.acme.domain.exceptions.ServiceException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Obligación a cumplir. Puede existir sin factura (VTV, renovaciones, impuestos). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Obligation {
    private UUID id;
    private UUID userId;
    private UUID serviceId;
    private UUID invoiceId;
    private UUID assetId;
    private UUID recurrenceId;
    /** Si es la cuota de un préstamo (módulo 13): el préstamo y su número de cuota. */
    private UUID loanId;
    private Integer installmentNumber;
    private String title;
    private String description;
    @Builder.Default
    private ObligationType obligationType = ObligationType.INVOICE_PAYMENT;
    private BigDecimal amount;
    private String currency;
    private LocalDate dueDate;
    @Builder.Default
    private ObligationStatus status = ObligationStatus.PENDING;
    /** true = anticipada por una regla de recurrencia, todavía sin factura real (CU-014). */
    private boolean estimated;
    private Instant cancelledAt;
    private String cancelReason;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Estado según lo pagado (pagos vigentes): nada → PENDING; todo o más, o sin importe → PAID;
     * si no, PARTIALLY_PAID. Lo usan el registro y la anulación de pagos.
     */
    public void applyPaidAmount(BigDecimal paid, Instant now) {
        if (status == ObligationStatus.CANCELLED) {
            throw new ServiceException("La obligación está cancelada");
        }
        if (paid.signum() > 0) {
            // Pagarlo confirma el importe: deja de ser estimado (la base no admite estimado y pagado)
            estimated = false;
        }
        if (paid.signum() <= 0) {
            status = ObligationStatus.PENDING;
            paidAt = null;
        } else if (amount == null || paid.compareTo(amount) >= 0) {
            if (status != ObligationStatus.PAID) {
                paidAt = now;
            }
            status = ObligationStatus.PAID;
        } else {
            status = ObligationStatus.PARTIALLY_PAID;
            paidAt = null;
        }
    }

    public void cancel(String reason, Instant now) {
        if (status == ObligationStatus.PAID) {
            throw new ServiceException("No se puede cancelar una obligación pagada");
        }
        status = ObligationStatus.CANCELLED;
        cancelReason = reason;
        cancelledAt = now;
    }
}
