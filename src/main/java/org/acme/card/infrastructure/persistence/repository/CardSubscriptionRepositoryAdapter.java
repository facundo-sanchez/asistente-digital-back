package org.acme.card.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.acme.card.application.model.SubscriptionCharge;
import org.acme.card.application.port.output.CardSubscriptionRepository;
import org.acme.card.domain.model.CardTransactionType;
import org.acme.card.domain.model.SpendingCategory;
import org.acme.card.infrastructure.persistence.entity.CardSubscriptionDismissalEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Compras sin cuotas de v_card_spending (resúmenes confirmados) y los comercios descartados. */
@ApplicationScoped
public class CardSubscriptionRepositoryAdapter implements CardSubscriptionRepository {

    @Inject
    EntityManager em;

    @Override
    public List<SubscriptionCharge> charges(UUID userId, UUID cardId, LocalDate since) {
        TypedQuery<Object[]> query = em.createQuery("select v.cardId, c.name, v.closingDate, v.transactionDate, v.merchant, v.category, v.currency, v.amount"
                        + " from CardSpendingViewEntity v, CardTransactionEntity t, CardEntity c"
                        + " where t.id = v.id and c.id = v.cardId and v.userId = :userId and v.closingDate >= :since"
                        + " and v.transactionType = :purchase and (t.installmentTotal is null or t.installmentTotal <= 1)"
                        + (cardId != null ? " and v.cardId = :cardId" : ""), Object[].class)
                .setParameter("userId", userId).setParameter("since", since).setParameter("purchase", CardTransactionType.PURCHASE);
        if (cardId != null) {
            query.setParameter("cardId", cardId);
        }
        return query.getResultList().stream().map(r -> new SubscriptionCharge((UUID) r[0], (String) r[1], (LocalDate) r[2], (LocalDate) r[3],
                (String) r[4], (SpendingCategory) r[5], (String) r[6], (BigDecimal) r[7])).toList();
    }

    @Override
    public Set<String> dismissed(UUID userId) {
        return new HashSet<>(em.createQuery("select d.merchant from CardSubscriptionDismissalEntity d where d.userId = :userId", String.class)
                .setParameter("userId", userId).getResultList());
    }

    @Override
    @Transactional
    public void dismiss(UUID userId, String merchant, Instant now) {
        if (dismissed(userId).contains(merchant)) {
            return;
        }
        CardSubscriptionDismissalEntity entity = new CardSubscriptionDismissalEntity();
        entity.setUserId(userId);
        entity.setMerchant(merchant);
        entity.setCreatedAt(now);
        em.persist(entity);
    }

    @Override
    @Transactional
    public void restore(UUID userId, String merchant) {
        em.createQuery("delete from CardSubscriptionDismissalEntity d where d.userId = :userId and d.merchant = :merchant")
                .setParameter("userId", userId).setParameter("merchant", merchant).executeUpdate();
    }
}
