package org.acme.infrastructure.resources.rest.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;


public record UsuarioDTO(
        @Schema(description = "ID del Usuario", example = "1")
        Long id,

        @Schema(description = "Nombre del Usuario", example = "Facundo")
        @NotBlank(message = "El nombre del Usuario no puede estar vacío ni ser nulo")
        String nombre,

        @Schema(description = "Email", example = "google@gmail.com")
        @NotBlank(message = "El Email no puede estar vacío ni ser nulo")
        String email,

        @Schema(description = "Password", example = "**********")
        @NotBlank(message = "La Password no puede estar vacío ni ser nulo")
        String password
) {}
