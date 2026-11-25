package org.acme.payment.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "VoidPaymentRequest")
public record VoidPaymentRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500)
        @Schema(example = "Importe cargado por error") String reason
) {}
