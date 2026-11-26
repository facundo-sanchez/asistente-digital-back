package org.acme.card.application.model;

import org.acme.card.domain.model.Card;

/** Una tarjeta con el nombre del servicio de su emisor, lista para mostrar. */
public record CardView(Card card, String serviceName) {}
