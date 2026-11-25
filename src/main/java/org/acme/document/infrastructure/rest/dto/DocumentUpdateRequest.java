package org.acme.document.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.acme.shared.domain.model.DocumentType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/** Metadatos editables de un documento. El archivo se carga por POST /documents/upload (multipart). */
@Schema(name = "DocumentUpdateRequest")
public record DocumentUpdateRequest(
        @Size(max = 200)
        String title,

        @NotNull(message = "El tipo de documento es obligatorio")
        DocumentType documentType,

        UUID serviceId,
        UUID assetId,
        UUID obligationId,
        LocalDate expiresOn
) {}
