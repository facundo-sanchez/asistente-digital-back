package org.acme.service.infrastructure.rest.dto;

import org.acme.shared.domain.model.BillingFrequency;
import org.acme.shared.domain.model.ServiceCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Schema(name = "ServiceResponse")
public record ServiceResponse(
        UUID id,
        UUID providerId,
        @Schema(example = "Luz casa") String name,
        @Schema(description = "Nro. de cliente / socio / suministro", example = "123456") String customerReference,
        ServiceCategory category,
        BillingFrequency billingFrequency,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        String notes,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {}
