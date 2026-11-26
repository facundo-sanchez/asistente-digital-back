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
