package org.acme.user.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ForgotPasswordRequest")
public record ForgotPasswordRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 255)
        @Schema(example = "usuario@gmail.com") String email
) {}
