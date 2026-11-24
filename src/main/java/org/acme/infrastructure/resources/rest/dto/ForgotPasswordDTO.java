package org.acme.infrastructure.resources.rest.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;


public record ForgotPasswordDTO(
        @Schema(description = "Email", example = "google@gmail.com")
        @NotBlank(message = "El Email no puede estar vacío ni ser nulo")
        String email,

        @Schema(description = "Password", example = "**********")
        @NotBlank(message = "La Password no puede estar vacío ni ser nulo")
        String password
) {}
