package org.acme.card.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/** GET /cards/installments/pending: compras en cuotas sin terminar y lo que falta por moneda (RN-018). */
@Schema(name = "PendingInstallmentsResponse")
public record PendingInstallmentsResponse(List<PendingInstallmentResponse> items, List<CurrencyTotalResponse> totals) {
}
