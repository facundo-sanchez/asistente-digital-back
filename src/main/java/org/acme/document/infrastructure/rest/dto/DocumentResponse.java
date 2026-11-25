package org.acme.document.infrastructure.rest.dto;

import org.acme.document.domain.model.DocumentSource;
import org.acme.document.domain.model.ProcessingStatus;
import org.acme.shared.domain.model.DocumentType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** No expone storageKey ni checksum. La descarga se hace por GET /documents/{id}/download. */
@Schema(name = "DocumentResponse")
public record DocumentResponse(
        UUID id,
        UUID serviceId,
        @Schema(description = "Nombre del servicio asociado (null si no tiene)", example = "Luz casa") String serviceName,
        UUID assetId,
        UUID obligationId,
        DocumentType documentType,
        @Schema(example = "Factura luz diciembre") String title,
        @Schema(example = "factura-12-2026.pdf") String originalFilename,
        @Schema(example = "application/pdf") String mimeType,
        @Schema(description = "Tamaño en bytes", example = "245760") long fileSize,
        DocumentSource source,
        ProcessingStatus processingStatus,
        LocalDate expiresOn,
        Instant createdAt,
        Instant updatedAt
) {}
