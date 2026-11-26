package org.acme.user.infrastructure.rest.dto;

import org.acme.user.domain.model.PaymentWallet;
import org.acme.user.domain.model.UserRole;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "UserResponse", description = "Perfil del usuario autenticado")
public record UserResponse(
        UUID id,
        @Schema(example = "usuario@gmail.com") String email,
        @Schema(example = "Facundo") String name,
        boolean emailVerified,
        @Schema(description = "false si la cuenta solo entra con Google: puede crear una contraseña sin indicar la actual")
        boolean hasPassword,
        @Schema(example = "America/Argentina/Buenos_Aires") String timezone,
        @Schema(example = "es-AR") String locale,
        @Schema(example = "ARS") String defaultCurrency,
        UserRole role,
        @Schema(description = "Billetera para pagar facturas (null: ninguna)") PaymentWallet paymentWallet,
        Instant createdAt
) {}
