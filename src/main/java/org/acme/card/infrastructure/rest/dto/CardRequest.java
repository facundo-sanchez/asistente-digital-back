package org.acme.card.infrastructure.rest.dto;

import jakarta.validation.constraints.*;
import org.acme.card.domain.model.CardBrand;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "CardRequest")
public record CardRequest(
        @NotNull(message = "El servicio del emisor es obligatorio")
        UUID serviceId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        @Schema(example = "Visa Galicia") String name,

        @NotNull(message = "La marca es obligatoria")
        CardBrand brand,

        @Pattern(regexp = "[0-9]{4}", message = "Deben ser los últimos 4 dígitos")
        @Schema(description = "Últimos 4 de la tarjeta", example = "1234") String lastFour,

        @Pattern(regexp = "[0-9]{4}", message = "Deben ser los últimos 4 dígitos de la cuenta")
        @Schema(description = "Últimos 4 del número de cuenta (figura en el email del resumen)", example = "4521")
        String accountLastFour,

        @Size(max = 150)
        String holderName,

        Boolean additional,

        @Min(1) @Max(31)
        Integer closingDay,

        @Min(1) @Max(31)
        Integer dueDay,

        @PositiveOrZero
        @Digits(integer = 15, fraction = 4)
        BigDecimal creditLimit,

        @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código ISO 4217")
        @Schema(description = "Moneda principal; por defecto ARS", example = "ARS") String currency
) {}
