package org.acme.dashboard.application.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Los números del Inicio (CU-012). El mes es el actual en la zona horaria del usuario (RN-012);
 * los importes van por moneda, sin sumarse ni convertirse (RN-018).
 *
 * @param withoutAmount vencimientos del mes sin importe cargado (no entran en los totales)
 * @param activity      lo último registrado (10 como mucho), lo más nuevo primero
 * @param dueSoon       cuántos vencen en los próximos 7 días
 */
public record DashboardSummary(
        LocalDate monthStart,
        LocalDate monthEnd,
        List<MonthTotals> month,
        long withoutAmount,
        List<OverdueTotals> overdue,
        long dueSoon,
        long activeServices,
        List<PendingReview> toReview,
        List<ExpiringDocument> expiringDocuments,
        List<Activity> activity
) {}
