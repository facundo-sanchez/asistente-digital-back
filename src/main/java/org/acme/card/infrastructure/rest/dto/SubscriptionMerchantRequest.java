package org.acme.card.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** El comercio a ocultar o volver a mostrar en Suscripciones. */
@Schema(name = "SubscriptionMerchantRequest")
public record SubscriptionMerchantRequest(@NotBlank(message = "El comercio es obligatorio") @Schema(example = "SUPERMERCADO DIA") String merchant) {}
