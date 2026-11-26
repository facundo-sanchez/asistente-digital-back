package org.acme.card.application.usecase;

import org.acme.card.application.port.output.MerchantRuleRepository;
import org.acme.card.domain.model.CardMerchantRule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryMerchantRuleRepository implements MerchantRuleRepository {

    public final Map<UUID, CardMerchantRule> rows = new LinkedHashMap<>();

    @Override
    public List<CardMerchantRule> findForUser(UUID userId) {
        return rows.values().stream().filter(r -> r.getUserId().equals(userId)).map(r -> r.toBuilder().build()).toList();
    }

    @Override
    public Optional<CardMerchantRule> findByMerchant(UUID userId, String merchantName) {
        return findForUser(userId).stream().filter(r -> r.getMerchantName().equals(merchantName)).findFirst();
    }

    @Override
    public CardMerchantRule save(CardMerchantRule rule) {
        CardMerchantRule copy = rule.toBuilder().build();
        if (copy.getId() == null) copy.setId(UUID.randomUUID());
        rows.put(copy.getId(), copy);
        return copy.toBuilder().build();
    }

    @Override
    public boolean delete(UUID userId, UUID ruleId) {
        CardMerchantRule rule = rows.get(ruleId);
        return rule != null && rule.getUserId().equals(userId) && rows.remove(ruleId) != null;
    }
}
