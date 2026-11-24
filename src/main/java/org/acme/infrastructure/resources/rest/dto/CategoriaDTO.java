package org.acme.infrastructure.resources.rest.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;


public record CategoriaDTO(
        @Schema(description = "ID de la Categoria", example = "1")
        Long id,
        @Schema(description = "Nombre de la Categoria", example = "Compra / Servicio")
        @NotBlank(message = "El nombre de la Categoria no puede estar vacío ni ser nulo")
        String nombre,
        @Schema(description = "Color de la Categoria", example = "primary")
        String color,
        @Schema(description = "Estado de la Categoria", example = "true")
        Boolean activo,
        @Schema(description = "Si es un Gasto Fijo en la Categoria", example = "true")
        Boolean gastoFijo,
        @Schema(description = "Estado de la base de la Categoria", example = "true")
        Boolean borrado
) {
    public CategoriaDTO {
        if (activo == null) {
            activo = true;
        }
        if (borrado == null) {
            borrado = false;
        }
        if (gastoFijo == null) {
            gastoFijo = false;
        }
    }
}
