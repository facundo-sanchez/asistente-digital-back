package org.acme.card.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.acme.card.application.model.SpendingFilter;
import org.acme.card.application.port.output.CardSpendingRepository;
import org.acme.card.domain.model.CardSpending;
import org.acme.card.domain.model.CardTransaction;
import org.acme.card.domain.model.CategorySpending;
import org.acme.card.domain.model.CurrencyTotal;
import org.acme.card.domain.model.MerchantSpending;
import org.acme.card.domain.model.MonthlySpending;
import org.acme.card.domain.model.PendingInstallment;
import org.acme.card.domain.model.SpendingCategory;
import org.acme.card.infrastructure.persistence.entity.CardTransactionEntity;
import org.acme.card.infrastructure.persistence.entity.PendingInstallmentViewEntity;
import org.acme.card.infrastructure.persistence.mapper.CardPersistenceMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Consultas agrupadas sobre la vista v_card_spending (CardSpendingViewEntity). */
@ApplicationScoped
public class CardSpendingRepositoryAdapter implements CardSpendingRepository {

    @Inject
    EntityManager em;

    @Inject
    CardPersistenceMapper mapper;

    @Override
    public List<CurrencyTotal> totals(UUID userId, SpendingFilter filter) {
        return query("select v.currency, sum(v.amount), count(v) from CardSpendingViewEntity v where " + where(filter)
                + " group by v.currency order by v.currency", userId, filter)
                .getResultList().stream().map(r -> new CurrencyTotal((String) r[0], (BigDecimal) r[1], (Long) r[2])).toList();
    }

    @Override
    public List<CategorySpending> byCategory(UUID userId, SpendingFilter filter) {
        return query("select v.category, v.currency, sum(v.amount), count(v) from CardSpendingViewEntity v where " + where(filter)
                + " group by v.category, v.currency order by sum(v.amount) desc", userId, filter)
                .getResultList().stream().map(r -> new CategorySpending((SpendingCategory) r[0], (String) r[1], (BigDecimal) r[2], (Long) r[3])).toList();
    }

    @Override
    public List<MonthlySpending> byMonth(UUID userId, SpendingFilter filter) {
        return query("select v.billingMonth, v.currency, sum(v.amount), count(v) from CardSpendingViewEntity v where " + where(filter)
                + " group by v.billingMonth, v.currency order by v.billingMonth", userId, filter)
                .getResultList().stream().map(r -> new MonthlySpending((LocalDate) r[0], (String) r[1], (BigDecimal) r[2], (Long) r[3])).toList();
    }

    @Override
    public List<CardSpending> byCard(UUID userId, SpendingFilter filter) {
        return query("select v.cardId, c.name, v.currency, sum(v.amount), count(v) from CardSpendingViewEntity v, CardEntity c where c.id = v.cardId and "
                + where(filter) + " group by v.cardId, c.name, v.currency order by sum(v.amount) desc", userId, filter)
                .getResultList().stream().map(r -> new CardSpending((UUID) r[0], (String) r[1], (String) r[2], (BigDecimal) r[3], (Long) r[4])).toList();
    }

    @Override
    public List<MerchantSpending> topMerchants(UUID userId, SpendingFilter filter, int limit) {
        return query("select v.merchant, v.currency, sum(v.amount), count(v) from CardSpendingViewEntity v where " + where(filter)
                + " group by v.merchant, v.currency order by sum(v.amount) desc", userId, filter)
                .setMaxResults(limit)
                .getResultList().stream().map(r -> new MerchantSpending((String) r[0], (String) r[1], (BigDecimal) r[2], (Long) r[3])).toList();
    }

    @Override
    public PageResult<CardTransaction> transactions(UUID userId, SpendingFilter filter, PageQuery page) {
        String where = where(filter);
        long total = (Long) bind(em.createQuery("select count(v) from CardSpendingViewEntity v where " + where), userId, filter).getSingleResult();
        List<UUID> ids = bind(em.createQuery("select v.id from CardSpendingViewEntity v where " + where
                + " order by v.transactionDate desc, v.id", UUID.class), userId, filter)
                .setFirstResult(page.page() * page.size()).setMaxResults(page.size()).getResultList();
        List<CardTransaction> content = ids.isEmpty() ? List.of()
                : em.createQuery("from CardTransactionEntity t where t.id in :ids", CardTransactionEntity.class).setParameter("ids", ids)
                .getResultList().stream().map(mapper::toModel)
                .sorted(Comparator.comparing((CardTransaction t) -> ids.indexOf(t.getId()))).toList();
        return new PageResult<>(content, page.page(), page.size(), total);
    }

    @Override
    public List<PendingInstallment> pendingInstallments(UUID userId) {
        return em.createQuery("from PendingInstallmentViewEntity p where p.userId = :userId order by p.estimatedLastClosingDate, p.merchant",
                        PendingInstallmentViewEntity.class)
                .setParameter("userId", userId).getResultList().stream().map(mapper::toModel).toList();
    }

    private static String where(SpendingFilter filter) {
        return "v.userId = :userId and v.billingMonth between :from and :to"
                + (filter.cardId() != null ? " and v.cardId = :cardId" : "")
                + (filter.category() != null ? " and v.category = :category" : "")
                + (filter.merchant() != null ? " and v.merchant = :merchant" : "");
    }

    private TypedQuery<Object[]> query(String jpql, UUID userId, SpendingFilter filter) {
        return bind(em.createQuery(jpql, Object[].class), userId, filter);
    }

    private static <T> T bind(T query, UUID userId, SpendingFilter filter) {
        jakarta.persistence.Query q = (jakarta.persistence.Query) query;
        q.setParameter("userId", userId).setParameter("from", filter.from()).setParameter("to", filter.to());
        if (filter.cardId() != null) q.setParameter("cardId", filter.cardId());
        if (filter.category() != null) q.setParameter("category", filter.category());
        if (filter.merchant() != null) q.setParameter("merchant", filter.merchant());
        return query;
    }
}
