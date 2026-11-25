package org.acme.obligation.application.usecase;

import org.acme.obligation.application.model.ObligationFilter;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio en memoria. paid simula los pagos vigentes. El estado efectivo es el
 * persistido: "vencida" y "próxima a vencer" los calcula la vista SQL (se prueban de punta a punta).
 */
public class InMemoryObligationRepository implements ObligationRepository {

    final Map<UUID, Obligation> rows = new LinkedHashMap<>();
    final Map<UUID, BigDecimal> paid = new HashMap<>();

    @Override
    public ObligationSummary save(Obligation obligation) {
        Obligation copy = obligation.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
        }
        rows.put(copy.getId(), copy);
        return summary(copy);
    }

    @Override
    public Optional<ObligationSummary> findByIdForUser(UUID userId, UUID obligationId) {
        return Optional.ofNullable(rows.get(obligationId)).filter(o -> o.getUserId().equals(userId)).map(this::summary);
    }

    @Override
    public PageResult<ObligationSummary> findForUser(UUID userId, ObligationFilter filter, PageQuery page) {
        List<ObligationSummary> matches = rows.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .filter(o -> filter.text() == null || o.getTitle().toLowerCase(Locale.ROOT).contains(filter.text().toLowerCase(Locale.ROOT)))
                .filter(o -> filter.serviceId() == null || filter.serviceId().equals(o.getServiceId()))
                .filter(o -> filter.from() == null || !o.getDueDate().isBefore(filter.from()))
                .filter(o -> filter.to() == null || !o.getDueDate().isAfter(filter.to()))
                .sorted(Comparator.comparing(Obligation::getDueDate))
                .map(this::summary)
                .filter(s -> filter.statuses() == null || filter.statuses().isEmpty() || filter.statuses().contains(s.effectiveStatus()))
                .toList();
        int from = Math.min(page.page() * page.size(), matches.size());
        int to = Math.min(from + page.size(), matches.size());
        return new PageResult<>(matches.subList(from, to), page.page(), page.size(), matches.size());
    }

    private ObligationSummary summary(Obligation obligation) {
        BigDecimal paidAmount = paid.getOrDefault(obligation.getId(), BigDecimal.ZERO);
        BigDecimal balance = obligation.getAmount() == null ? null : obligation.getAmount().subtract(paidAmount).max(BigDecimal.ZERO);
        return new ObligationSummary(obligation.toBuilder().build(), paidAmount, balance,
                EffectiveObligationStatus.valueOf(obligation.getStatus().name()));
    }
}
