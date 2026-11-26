package org.acme.user.infrastructure.rest.dto;

import org.acme.user.domain.model.PaymentWallet;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "PaymentWalletRequest")
public record PaymentWalletRequest(
        @Schema(description = "null para no usar ninguna") PaymentWallet paymentWallet
) {}
