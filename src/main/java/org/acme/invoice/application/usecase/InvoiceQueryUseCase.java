package org.acme.invoice.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.invoice.application.model.InvoiceFilter;
import org.acme.invoice.application.model.InvoiceView;
import org.acme.invoice.application.port.output.InvoiceRepository;
import org.acme.invoice.domain.model.Invoice;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.shared.application.SearchText;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Consulta de facturas del usuario, con su servicio y su vencimiento. */
@ApplicationScoped
public class InvoiceQueryUseCase {

    private final InvoiceRepository invoices;
    private final ServiceRepository services;
    private final ObligationRepository obligations;

    @Inject
    public InvoiceQueryUseCase(InvoiceRepository invoices, ServiceRepository services, ObligationRepository obligations) {
        this.invoices = invoices;
        this.services = services;
        this.obligations = obligations;
    }

    public PageResult<InvoiceView> list(UUID userId, InvoiceFilter filter, PageQuery page) {
        InvoiceFilter normalized = new InvoiceFilter(SearchText.normalize(filter.text()), filter.serviceId(),
                filter.status(), filter.from(), filter.to());
        PageResult<Invoice> result = invoices.findForUser(userId, normalized, page);
        // Dos consultas por página: nombres de servicio y vencimientos
        Map<UUID, String> names = services.findNames(userId, result.content().stream().map(Invoice::getServiceId).collect(Collectors.toSet()));
        Map<UUID, ObligationSummary> generated = obligations.findByInvoices(userId, result.content().stream().map(Invoice::getId).toList());
        return result.map(invoice -> new InvoiceView(invoice, names.get(invoice.getServiceId()), generated.get(invoice.getId())));
    }

    /** Una factura de otro usuario responde igual que una inexistente. */
    public InvoiceView get(UUID userId, UUID invoiceId) {
        Invoice invoice = invoices.findByIdForUser(userId, invoiceId).orElseThrow(() -> new NotFoundException("factura"));
        String serviceName = services.findNames(userId, Set.of(invoice.getServiceId())).get(invoice.getServiceId());
        ObligationSummary obligation = obligations.findByInvoices(userId, Set.of(invoice.getId())).get(invoice.getId());
        return new InvoiceView(invoice, serviceName, obligation);
    }

    /** Posibles repetidas para avisar antes de guardar (RN-005). excludeId: al editar, la propia factura no cuenta. */
    public List<InvoiceView> duplicates(UUID userId, UUID serviceId, String invoiceNumber, String billingPeriod,
                                        BigDecimal totalAmount, UUID excludeId) {
        String serviceName = services.findNames(userId, Set.of(serviceId)).get(serviceId);
        if (serviceName == null) {
            return List.of();
        }
        return invoices.findPossibleDuplicates(userId, serviceId, blankToNull(invoiceNumber), blankToNull(billingPeriod), totalAmount).stream()
                .filter(invoice -> !invoice.getId().equals(excludeId))
                .map(invoice -> new InvoiceView(invoice, serviceName, null))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
