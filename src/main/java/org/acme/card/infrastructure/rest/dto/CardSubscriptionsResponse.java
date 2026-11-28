package org.acme.card.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/** GET /cards/subscriptions: las suscripciones, el total mensual por moneda y los comercios ocultos. */
@Schema(name = "CardSubscriptionsResponse")
public record CardSubscriptionsResponse(List<CardSubscriptionResponse> items, List<CurrencyTotalResponse> totals, List<String> hidden) {
}
