package org.acme.invoice.application.port.output;

import org.acme.invoice.application.model.InvoiceFilter;
import org.acme.invoice.domain.model.Invoice;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Facturas de un usuario. Toda consulta va filtrada por userId. */
public interface InvoiceRepository {

    /** Inserta si no tiene id; si no, actualiza. */
    Invoice save(Invoice invoice);

    /** Las confirmadas del servicio en esa moneda que vencen antes de before, sin la factura dada, de la más nueva a la más vieja. */
    List<Invoice> findLastConfirmed(UUID userId, UUID serviceId, String currency, UUID excludeId, java.time.LocalDate before, int limit);

    /** La primera confirmada del servicio en esa moneda que vence después de after (sin la factura dada). */
    Optional<Invoice> findNextConfirmed(UUID userId, UUID serviceId, String currency, UUID excludeId, java.time.LocalDate after);

    Optional<Invoice> findByIdForUser(UUID userId, UUID invoiceId);

    /** Por vencimiento, más nuevas primero. */
    PageResult<Invoice> findForUser(UUID userId, InvoiceFilter filter, PageQuery page);

    /** Una factura confirmada del servicio con ese número (RN-005). */
    Optional<Invoice> findConfirmedByNumber(UUID userId, UUID serviceId, String invoiceNumber);

    /**
     * Confirmadas del servicio que podrían ser la misma factura (RN-005): mismo número, o mismo
     * período e importe. Los criterios en null no se usan.
     */
    List<Invoice> findPossibleDuplicates(UUID userId, UUID serviceId, String invoiceNumber, String billingPeriod, BigDecimal totalAmount);

    /** La factura creada al confirmar esa propuesta de la IA. */
    Optional<Invoice> findByExtraction(UUID userId, UUID extractionResultId);
}
