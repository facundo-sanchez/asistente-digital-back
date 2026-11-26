package org.acme.document.infrastructure.rest.dto;

import org.acme.document.domain.model.ProcessingJobStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Estado del procesamiento (GET /documents/{id}/processing). */
@Schema(name = "ProcessingJobResponse")
public record ProcessingJobResponse(
        UUID id,
        UUID documentId,
        ProcessingJobStatus status,
        int attempts,
        int maxAttempts,
        @Schema(example = "OCR_UNREADABLE") String errorCode,
        @Schema(description = "Próximo reintento automático, si falló y quedan intentos") Instant nextRetryAt,
        Instant startedAt,
        Instant createdAt,
        Instant completedAt
) {}
