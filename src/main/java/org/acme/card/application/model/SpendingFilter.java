package org.acme.card.application.model;

import org.acme.card.domain.model.SpendingCategory;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtro del informe y de su detalle: meses de facturación desde/hasta (primer día del mes, inclusive) y, opcionales,
 * tarjeta, rubro y comercio.
 */
public record SpendingFilter(LocalDate from, LocalDate to, UUID cardId, SpendingCategory category, String merchant) {
}
