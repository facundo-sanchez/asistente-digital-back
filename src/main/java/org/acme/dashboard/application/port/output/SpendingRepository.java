package org.acme.dashboard.application.port.output;

import org.acme.dashboard.application.model.SpendingTotal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Sumas de los pagos vigentes (no anulados) del usuario entre dos fechas, inclusive. */
public interface SpendingRepository {

    /** key = mes "YYYY-MM". */
    List<SpendingTotal> paidByMonth(UUID userId, LocalDate from, LocalDate to);

    /** key = categoría del servicio del vencimiento; OTHER si no tiene servicio. */
    List<SpendingTotal> paidByCategory(UUID userId, LocalDate from, LocalDate to);
}
