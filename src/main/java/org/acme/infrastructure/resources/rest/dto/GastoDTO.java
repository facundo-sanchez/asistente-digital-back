package org.acme.infrastructure.resources.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoDTO(
        @Schema(description = "ID del producto", example = "1")
        Long id,

        @Schema(description = "Nombre del producto", example = "Televisor")
        @NotBlank(message = "El nombre del producto no puede estar vacío ni ser nulo")
        String producto,

        @Schema(description = "Numero de cuotas", example = "12")
//        @NotNull(message = "El numero de cuotas no puede estar vacío ni ser nulo")
        int cuotas,

        @Schema(description = "Precio de la cuota", example = "1500000.00")
        @Positive(message = "El precio de la cuota debe ser mayor a cero")
        @NotNull(message = "El precio obligatorio")
        BigDecimal precio,

        @Schema(description = "ID Tarjeta")
        @NotNull(message = "La Tarjeta obligatorio")
        Long tarjetaId,

        @Schema(description = "ID Categoria")
        @NotNull(message = "La Categoria obligatorio")
        Long categoriaId,

        @Schema(description = "Fecha de fin laboral (opcional)", example = "2026-08-16")
        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio, // Opcional: sin @NotNull

        @Schema(description = "Si el Gasto esta pagada", example = "false")
        Boolean pagada // Opcional: sin @NotNull

) {}
