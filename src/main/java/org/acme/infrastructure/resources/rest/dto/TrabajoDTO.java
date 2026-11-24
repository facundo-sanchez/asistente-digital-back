package org.acme.infrastructure.resources.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrabajoDTO(
        @Schema(description = "ID del trabajo", example = "1")
        Long id,

        @Schema(description = "Nombre del trabajo", example = "Desarrollador Java")
        @NotBlank(message = "El nombre del puesto no puede estar vacío ni ser nulo")
        String puesto,

        @Schema(description = "Nombre de la empresa", example = "Google")
        @NotBlank(message = "El nombre de la empresa no puede estar vacío ni ser nulo")
        String empresa,

        @Schema(description = "Sueldo ofrecido", example = "1500000.00")
        @NotNull(message = "El sueldo es obligatorio")
        @Positive(message = "El sueldo debe ser mayor a cero")
        BigDecimal sueldoBruto,

        @Schema(description = "Sueldo neto", example = "1500000.00")
        @NotNull(message = "El sueldo es obligatorio")
        @Positive(message = "El sueldo debe ser mayor a cero")
        BigDecimal sueldoNeto,

        @Schema(description = "Fecha de inicio laboral", example = "2024-01-15T09:00:00")
        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime fechaInicio,

        @Schema(description = "Fecha de fin laboral (opcional)", example = "2026-08-16T18:00:00")
        LocalDateTime fechaFin // Opcional: sin @NotNull
) {}
