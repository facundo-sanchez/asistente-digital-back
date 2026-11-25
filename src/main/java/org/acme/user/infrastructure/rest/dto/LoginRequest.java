package org.acme.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "LoginRequest")
public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Size(max = 255)
        @Schema(example = "usuario@gmail.com") String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 72)
        String password
) {}
