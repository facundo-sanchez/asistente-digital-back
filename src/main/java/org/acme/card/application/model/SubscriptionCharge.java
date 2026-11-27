package org.acme.card.application.model;

import org.acme.card.domain.model.SpendingCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Un consumo sin cuotas de un resumen confirmado, candidato a suscripción. */
public record SubscriptionCharge(UUID cardId, String cardName, LocalDate closingDate, LocalDate transactionDate, String merchant,
                                 SpendingCategory category, String currency, BigDecimal amount) {
}
