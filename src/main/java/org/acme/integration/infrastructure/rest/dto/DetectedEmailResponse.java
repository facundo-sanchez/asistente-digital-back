package org.acme.integration.infrastructure.rest.dto;

import org.acme.integration.domain.model.ProcessedItemResult;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Un correo analizado, para la pantalla "Detectado en tu correo". */
@Schema(name = "DetectedEmailResponse")
public record DetectedEmailResponse(
        UUID id,
        UUID integrationId,
        UUID documentId,
        UUID providerId,
        UUID serviceId,
        @Schema(example = "facturacion@coopelectric.com.ar") String sender,
        @Schema(example = "Su factura de diciembre") String subject,
        Instant receivedAt,
        ProcessedItemResult result,
        @Schema(example = "UNKNOWN_CUSTOMER") String discardReason,
        Instant processedAt
) {}
