package org.acme.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "UpdateProfileRequest")
public record UpdateProfileRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        @Schema(example = "Facundo") String name,

        @NotBlank(message = "La zona horaria es obligatoria")
        @Size(max = 60)
        @Schema(example = "America/Argentina/Buenos_Aires") String timezone,

        @NotBlank(message = "El locale es obligatorio")
        @Size(max = 10)
        @Schema(example = "es-AR") String locale,

        @NotBlank(message = "La moneda es obligatoria")
        @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código ISO 4217")
        @Schema(example = "ARS") String defaultCurrency
) {}
