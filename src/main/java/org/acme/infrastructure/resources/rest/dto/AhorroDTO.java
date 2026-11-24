package org.acme.infrastructure.resources.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;


public record AhorroDTO(
        @Schema(description = "ID del Ahorro", example = "1")
        Long id,
        @Schema(description = "Nombre del Ahorro", example = "Auto")
        @NotBlank(message = "El nombre del Ahorro no puede estar vacío ni ser nulo")
        String motivo,
        @Schema(description = "Importe del Ahorro", example = "1500000.00")
        @Positive(message = "El Importe debe ser mayor a cero")
        @NotNull(message = "El Importe es obligatorio")
        BigDecimal importe,
        @Schema(description = "Periodo del Ahorro", example = "09/2026")
        @NotBlank(message = "El Periodo del Ahorro no puede estar vacío ni ser nulo")
        String periodo,
        @Schema(description = "Estado del Ahorro", example = "true")
        Boolean activo,
        @Schema(description = "Estado de la base del Ahorro", example = "true")
        Boolean borrado
) { }
