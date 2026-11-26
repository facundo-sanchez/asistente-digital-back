package org.acme.dashboard.application.model;

import java.util.List;

/** Para los gráficos: pagado por mes (los últimos 6, con ceros) y pagado este mes por categoría. */
public record DashboardSpending(List<SpendingTotal> months, List<SpendingTotal> categories) {
}
