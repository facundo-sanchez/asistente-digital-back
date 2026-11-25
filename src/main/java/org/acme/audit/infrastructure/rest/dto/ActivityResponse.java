package org.acme.audit.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** "Últimas actividades" del dashboard (CU-012). No expone IP ni correlation id. */
@Schema(name = "ActivityResponse")
public record ActivityResponse(
        UUID id,
        @Schema(example = "PAYMENT_REGISTERED") String action,
        @Schema(example = "PAYMENT") String entityType,
        UUID entityId,
        Map<String, Object> metadata,
        Instant createdAt
) {}
