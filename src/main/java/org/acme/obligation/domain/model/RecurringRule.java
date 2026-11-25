package org.acme.obligation.domain.model;

import lombok.*;
import org.acme.shared.domain.model.BillingFrequency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Regla de recurrencia para anticipar obligaciones periódicas (CU-014). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RecurringRule {
    private UUID id;
    private UUID userId;
    private UUID serviceId;
    private UUID assetId;
    private String title;
    /** NONE no es válido para una regla. */
    private BillingFrequency frequency;
    private Integer dayOfMonth;
    private LocalDate nextExpectedDate;
    private BigDecimal estimatedAmount;
    private String currency;
    @Builder.Default
    private RecurrenceOrigin origin = RecurrenceOrigin.USER;
    @Builder.Default
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;
}
