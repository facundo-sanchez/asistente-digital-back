package org.acme.card.application.port.output;

import org.acme.card.domain.model.CardMerchantRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Rubro elegido por el usuario para un comercio (RN-019). merchantName ya normalizado (mayúsculas). */
public interface MerchantRuleRepository {

    List<CardMerchantRule> findForUser(UUID userId);

    Optional<CardMerchantRule> findByMerchant(UUID userId, String merchantName);

    CardMerchantRule save(CardMerchantRule rule);

    /** false si no existe o es de otro usuario. */
    boolean delete(UUID userId, UUID ruleId);
}
