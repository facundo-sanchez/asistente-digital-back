package org.acme.obligation.application.port.output;

import org.acme.obligation.application.model.ObligationFilter;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Obligaciones de un usuario. Las lecturas salen de v_obligations (saldo y estado efectivo). */
public interface ObligationRepository {

    /** Inserta si no tiene id; si no, actualiza. Devuelve su resumen recién calculado. */
    ObligationSummary save(Obligation obligation);

    Optional<ObligationSummary> findByIdForUser(UUID userId, UUID obligationId);

    /** Por vencimiento, las más próximas (y las vencidas) primero. */
    PageResult<ObligationSummary> findForUser(UUID userId, ObligationFilter filter, PageQuery page);

    /** true si la regla ya generó el vencimiento de esa fecha. */
    boolean existsForRecurrence(UUID recurrenceId, LocalDate dueDate);

    /** Vencimientos generados por esas facturas del usuario, por id de factura. */
    Map<UUID, ObligationSummary> findByInvoices(UUID userId, Collection<UUID> invoiceIds);
}
