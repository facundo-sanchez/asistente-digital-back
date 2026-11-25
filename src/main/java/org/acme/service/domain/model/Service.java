package org.acme.service.domain.model;

import lombok.*;
import org.acme.shared.domain.model.BillingFrequency;
import org.acme.shared.domain.model.ServiceCategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Servicio contratado o administrado por el usuario (CU-001). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    private UUID id;
    private UUID userId;
    private UUID providerId;
    private String name;
    /** Número de cliente / socio / suministro. Asocia las facturas detectadas al servicio. */
    private String customerReference;
    private ServiceCategory category;
    @Builder.Default
    private BillingFrequency billingFrequency = BillingFrequency.MONTHLY;
    private LocalDate startDate;
    private LocalDate endDate;
    @Builder.Default
    private boolean active = true;
    private String notes;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    private Instant createdAt;
    private Instant updatedAt;
}
