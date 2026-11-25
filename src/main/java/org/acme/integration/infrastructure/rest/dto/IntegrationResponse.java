package org.acme.integration.infrastructure.rest.dto;

import org.acme.integration.domain.model.IntegrationProvider;
import org.acme.integration.domain.model.IntegrationStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** No expone tokens, cursor de sincronización ni detalle técnico de errores. */
@Schema(name = "IntegrationResponse")
public record IntegrationResponse(
        UUID id,
        IntegrationProvider provider,
        @Schema(example = "usuario@gmail.com") String externalAccountId,
        IntegrationStatus status,
        @Schema(example = "[\"https://www.googleapis.com/auth/gmail.readonly\"]") List<String> scopes,
        LocalDate syncFrom,
        Instant lastSyncAt,
        Instant disconnectedAt,
        Instant createdAt,
        Instant updatedAt
) {}
