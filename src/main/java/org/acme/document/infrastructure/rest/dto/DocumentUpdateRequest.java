package org.acme.document.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.acme.shared.domain.model.DocumentType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/** Datos editables de un documento (PUT /documents/{id}). El archivo no se cambia. */
@Schema(name = "DocumentUpdateRequest")
public record DocumentUpdateRequest(
        @Size(max = 200, message = "El título admite hasta 200 caracteres")
        @Schema(example = "Factura luz diciembre")
        String title,

        @NotNull(message = "El tipo de documento es obligatorio")
        DocumentType documentType,

        @Schema(description = "Servicio del usuario. null lo desasocia")
        UUID serviceId,

        @Schema(description = "null si no vence", example = "2026-12-15")
        LocalDate expiresOn
) {}
