package org.acme.infrastructure.resources.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;


public record TarjetaDTO(
        @Schema(description = "ID de la Tarjeta", example = "1")
        Long id,

        @Schema(description = "Nombre de la Tarjeta", example = "Visa")
        @NotBlank(message = "El nombre de la Tarjeta no puede estar vacío ni ser nulo")
        String nombre,

        @Schema(description = "Si la tarjeta es Credito", example = "true")
        @NotNull(message = "El nombre del puesto no puede estar vacío ni ser nulo")
        Boolean isCredito


) {}
