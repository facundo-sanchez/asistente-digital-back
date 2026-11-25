package org.acme.card.infrastructure.persistence.mapper;

import org.acme.card.domain.model.*;
import org.acme.card.infrastructure.persistence.entity.*;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface CardPersistenceMapper {

    Card toModel(CardEntity entity);
    CardEntity toEntity(Card model);

    CardStatement toModel(CardStatementEntity entity);
    CardStatementEntity toEntity(CardStatement model);

    CardTransaction toModel(CardTransactionEntity entity);
    CardTransactionEntity toEntity(CardTransaction model);

    CardMerchantRule toModel(CardMerchantRuleEntity entity);
    CardMerchantRuleEntity toEntity(CardMerchantRule model);

    /** Lectura desde v_card_pending_installments. */
    PendingInstallment toModel(PendingInstallmentViewEntity entity);
}
