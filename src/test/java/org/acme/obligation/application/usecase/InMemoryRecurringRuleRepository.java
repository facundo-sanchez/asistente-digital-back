package org.acme.obligation.application.usecase;

import org.acme.obligation.application.port.output.RecurringRuleRepository;
import org.acme.obligation.domain.model.RecurringRule;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Reglas en memoria. Guarda copias. */
public class InMemoryRecurringRuleRepository implements RecurringRuleRepository {

    final Map<UUID, RecurringRule> rows = new LinkedHashMap<>();

    @Override
    public RecurringRule save(RecurringRule rule) {
        RecurringRule copy = rule.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
        }
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<RecurringRule> findByIdForUser(UUID userId, UUID ruleId) {
        return Optional.ofNullable(rows.get(ruleId)).filter(r -> r.getUserId().equals(userId)).map(r -> r.toBuilder().build());
    }

    @Override
    public List<RecurringRule> findForUser(UUID userId) {
        return rows.values().stream().filter(r -> r.getUserId().equals(userId))
                .sorted(Comparator.comparing(RecurringRule::getTitle)).map(r -> r.toBuilder().build()).toList();
    }

    @Override
    public List<RecurringRule> findActiveDueUntil(LocalDate limit) {
        return rows.values().stream().filter(r -> r.isActive() && !r.getNextExpectedDate().isAfter(limit))
                .map(r -> r.toBuilder().build()).toList();
    }
}
