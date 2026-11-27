package org.acme.invoice.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.invoice.application.model.InvoiceFilter;
import org.acme.invoice.application.port.output.InvoiceRepository;
import org.acme.invoice.domain.model.Invoice;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.acme.invoice.infrastructure.persistence.entity.InvoiceEntity;
import org.acme.invoice.infrastructure.persistence.mapper.InvoicePersistenceMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.infrastructure.persistence.LikePattern;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link InvoiceRepository}. */
@ApplicationScoped
public class InvoiceRepositoryAdapter implements InvoiceRepository {

    @Inject
    InvoicePanacheRepository repository;

    @Inject
    InvoicePersistenceMapper mapper;

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceEntity entity = mapper.toEntity(invoice);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public List<Invoice> findLastConfirmed(UUID userId, UUID serviceId, String currency, UUID excludeId, int limit) {
        return repository.find("userId = ?1 and serviceId = ?2 and currency = ?3 and id <> ?4 and status = ?5",
                        io.quarkus.panache.common.Sort.descending("dueDate", "createdAt"), userId, serviceId, currency, excludeId, InvoiceStatus.CONFIRMED)
                .page(0, limit).list().stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Invoice> findByIdForUser(UUID userId, UUID invoiceId) {
        return repository.find("id = ?1 and userId = ?2", invoiceId, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public PageResult<Invoice> findForUser(UUID userId, InvoiceFilter filter, PageQuery page) {
        StringBuilder where = new StringBuilder("userId = :userId");
        Parameters params = Parameters.with("userId", userId);
        if (filter.text() != null) {
            where.append(" and lower(invoiceNumber) like :text").append(LikePattern.ESCAPE);
            params.and("text", LikePattern.contains(filter.text()));
        }
        if (filter.serviceId() != null) {
            where.append(" and serviceId = :serviceId");
            params.and("serviceId", filter.serviceId());
        }
        if (filter.status() != null) {
            where.append(" and status = :status");
            params.and("status", filter.status());
        }
        if (filter.from() != null) {
            where.append(" and dueDate >= :from");
            params.and("from", filter.from());
        }
        if (filter.to() != null) {
            where.append(" and dueDate <= :to");
            params.and("to", filter.to());
        }

        PanacheQuery<InvoiceEntity> query = repository.find(where.toString(), Sort.descending("dueDate").and("createdAt", Sort.Direction.Descending), params)
                .page(Page.of(page.page(), page.size()));
        return new PageResult<>(query.list().stream().map(mapper::toModel).toList(), page.page(), page.size(), query.count());
    }

    @Override
    public Optional<Invoice> findConfirmedByNumber(UUID userId, UUID serviceId, String invoiceNumber) {
        return repository.find("userId = ?1 and serviceId = ?2 and invoiceNumber = ?3 and status = ?4",
                        userId, serviceId, invoiceNumber, InvoiceStatus.CONFIRMED)
                .firstResultOptional()
                .map(mapper::toModel);
    }

    @Override
    public List<Invoice> findPossibleDuplicates(UUID userId, UUID serviceId, String invoiceNumber, String billingPeriod, BigDecimal totalAmount) {
        List<String> criteria = new ArrayList<>();
        Parameters params = Parameters.with("userId", userId).and("serviceId", serviceId).and("status", InvoiceStatus.CONFIRMED);
        if (invoiceNumber != null) {
            criteria.add("invoiceNumber = :number");
            params.and("number", invoiceNumber);
        }
        if (billingPeriod != null && totalAmount != null) {
            criteria.add("(billingPeriod = :period and totalAmount = :amount)");
            params.and("period", billingPeriod).and("amount", totalAmount);
        }
        if (criteria.isEmpty()) {
            return List.of();
        }
        String where = "userId = :userId and serviceId = :serviceId and status = :status and (" + String.join(" or ", criteria) + ")";
        return repository.list(where, Sort.descending("dueDate"), params).stream().map(mapper::toModel).toList();
    }

    @Override
    public Optional<Invoice> findByExtraction(UUID userId, UUID extractionResultId) {
        return repository.find("userId = ?1 and extractionResultId = ?2", userId, extractionResultId).firstResultOptional().map(mapper::toModel);
    }
}
