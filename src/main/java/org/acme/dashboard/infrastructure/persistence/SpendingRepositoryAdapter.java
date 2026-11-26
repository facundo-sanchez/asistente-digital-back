package org.acme.dashboard.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.acme.dashboard.application.model.SpendingTotal;
import org.acme.dashboard.application.port.output.SpendingRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SpendingRepositoryAdapter implements SpendingRepository {

    private static final String PAID = " FROM payments p JOIN obligations o ON o.id = p.obligation_id LEFT JOIN services s ON s.id = o.service_id"
            + " WHERE p.user_id = :userId AND p.voided_at IS NULL AND p.payment_date BETWEEN :from AND :to";

    @Inject
    EntityManager em;

    @Override
    public List<SpendingTotal> paidByMonth(UUID userId, LocalDate from, LocalDate to) {
        return totals("SELECT to_char(p.payment_date, 'YYYY-MM') AS k, p.currency, SUM(p.amount)" + PAID + " GROUP BY 1, 2 ORDER BY 1, 2", userId, from, to);
    }

    @Override
    public List<SpendingTotal> paidByCategory(UUID userId, LocalDate from, LocalDate to) {
        return totals("SELECT COALESCE(s.category, 'OTHER') AS k, p.currency, SUM(p.amount)" + PAID + " GROUP BY 1, 2 ORDER BY 3 DESC", userId, from, to);
    }

    @SuppressWarnings("unchecked")
    private List<SpendingTotal> totals(String sql, UUID userId, LocalDate from, LocalDate to) {
        List<Object[]> rows = em.createNativeQuery(sql).setParameter("userId", userId).setParameter("from", from).setParameter("to", to).getResultList();
        return rows.stream().map(r -> new SpendingTotal((String) r[0], r[1].toString().trim(), (BigDecimal) r[2])).toList();
    }
}
