package org.acme.invoice.application.usecase;

import org.acme.invoice.application.model.InvoiceFilter;
import org.acme.invoice.application.port.output.InvoiceRepository;
import org.acme.invoice.domain.model.Invoice;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Facturas en memoria con los mismos filtros que el adaptador real. Guarda copias. */
public class InMemoryInvoiceRepository implements InvoiceRepository {

    final Map<UUID, Invoice> rows = new LinkedHashMap<>();

    @Override
    public Invoice save(Invoice invoice) {
        Invoice copy = invoice.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
        }
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<Invoice> findByIdForUser(UUID userId, UUID invoiceId) {
        return Optional.ofNullable(rows.get(invoiceId)).filter(i -> i.getUserId().equals(userId)).map(i -> i.toBuilder().build());
    }

    @Override
    public PageResult<Invoice> findForUser(UUID userId, InvoiceFilter filter, PageQuery page) {
        List<Invoice> matches = rows.values().stream()
                .filter(i -> i.getUserId().equals(userId))
                .filter(i -> filter.text() == null || (i.getInvoiceNumber() != null
                        && i.getInvoiceNumber().toLowerCase(Locale.ROOT).contains(filter.text().toLowerCase(Locale.ROOT))))
                .filter(i -> filter.serviceId() == null || filter.serviceId().equals(i.getServiceId()))
                .filter(i -> filter.status() == null || filter.status() == i.getStatus())
                .filter(i -> filter.from() == null || !i.getDueDate().isBefore(filter.from()))
                .filter(i -> filter.to() == null || !i.getDueDate().isAfter(filter.to()))
                .sorted(Comparator.comparing(Invoice::getDueDate).reversed())
                .map(i -> i.toBuilder().build())
                .toList();
        int from = Math.min(page.page() * page.size(), matches.size());
        int to = Math.min(from + page.size(), matches.size());
        return new PageResult<>(matches.subList(from, to), page.page(), page.size(), matches.size());
    }

    @Override
    public Optional<Invoice> findConfirmedByNumber(UUID userId, UUID serviceId, String invoiceNumber) {
        return rows.values().stream()
                .filter(i -> i.getUserId().equals(userId) && i.getServiceId().equals(serviceId)
                        && invoiceNumber.equals(i.getInvoiceNumber()) && i.getStatus() == InvoiceStatus.CONFIRMED)
                .findFirst();
    }

    @Override
    public List<Invoice> findPossibleDuplicates(UUID userId, UUID serviceId, String invoiceNumber, String billingPeriod, BigDecimal totalAmount) {
        return rows.values().stream()
                .filter(i -> i.getUserId().equals(userId) && i.getServiceId().equals(serviceId) && i.getStatus() == InvoiceStatus.CONFIRMED)
                .filter(i -> (invoiceNumber != null && invoiceNumber.equals(i.getInvoiceNumber()))
                        || (billingPeriod != null && totalAmount != null && billingPeriod.equals(i.getBillingPeriod())
                        && totalAmount.compareTo(i.getTotalAmount()) == 0))
                .toList();
    }

    @Override
    public Optional<Invoice> findByExtraction(UUID userId, UUID extractionResultId) {
        return rows.values().stream().filter(i -> i.getUserId().equals(userId) && extractionResultId.equals(i.getExtractionResultId())).findFirst();
    }
}
