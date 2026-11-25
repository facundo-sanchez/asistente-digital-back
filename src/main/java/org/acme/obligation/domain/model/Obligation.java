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

    public void cancel(String reason, Instant now) {
        if (status == ObligationStatus.PAID) {
            throw new ServiceException("No se puede cancelar una obligación pagada");
        }
        status = ObligationStatus.CANCELLED;
        cancelReason = reason;
        cancelledAt = now;
    }
}
