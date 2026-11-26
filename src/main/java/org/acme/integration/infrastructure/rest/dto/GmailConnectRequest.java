package org.acme.integration.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

/** Vuelta de Google (POST /integrations/gmail): el BFF manda el código, su verifier PKCE y desde cuándo importar. */
@Schema(name = "GmailConnectRequest")
public record GmailConnectRequest(
        @NotBlank(message = "Falta el código de Google")
        String code,
        @NotBlank(message = "Falta el code verifier")
        String codeVerifier,
        @PastOrPresent(message = "La fecha no puede ser futura")
        @Schema(description = "Importar correos desde esta fecha. null = solo correos nuevos", example = "2026-08-24")
        LocalDate syncFrom
) {}
