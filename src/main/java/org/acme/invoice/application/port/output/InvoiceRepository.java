package org.acme.invoice.application.port.output;

import org.acme.invoice.application.model.InvoiceFilter;
import org.acme.invoice.domain.model.Invoice;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

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
}
