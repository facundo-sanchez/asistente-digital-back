package org.acme.integration.infrastructure.rest.dto;

import jakarta.validation.constraints.PastOrPresent;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

/** Inicio de la conexión con Gmail (POST /integrations/gmail/connect). */
@Schema(name = "GmailConnectRequest")
public record GmailConnectRequest(
        @PastOrPresent(message = "La fecha no puede ser futura")
        @Schema(description = "Importar correos desde esta fecha. null = solo correos nuevos", example = "2026-08-24")
        LocalDate syncFrom
) {}
