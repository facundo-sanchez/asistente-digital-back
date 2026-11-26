package org.acme.card.application.model;

import org.acme.card.domain.model.CurrencyTotal;
import org.acme.card.domain.model.PendingInstallment;

import java.util.List;

/** Compras en cuotas que no terminaron y cuánto falta, por moneda (RN-018). */
public record PendingInstallments(List<PendingInstallment> items, List<CurrencyTotal> totals) {
}
