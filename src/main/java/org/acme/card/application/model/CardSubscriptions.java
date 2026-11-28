package org.acme.card.application.model;

import org.acme.card.domain.model.CardSubscription;
import org.acme.card.domain.model.CurrencyTotal;

import java.util.List;

/** Las suscripciones, cuánto suman por mes en cada moneda y los comercios que el usuario ocultó. */
public record CardSubscriptions(List<CardSubscription> items, List<CurrencyTotal> totals, List<String> hidden) {
}
