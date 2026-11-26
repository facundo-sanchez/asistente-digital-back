package org.acme.dashboard.application.model;

import java.math.BigDecimal;

/** Lo pagado en una moneda, agrupado por key: el mes ("2026-11") o la categoría del servicio. */
public record SpendingTotal(String key, String currency, BigDecimal total) {
}
