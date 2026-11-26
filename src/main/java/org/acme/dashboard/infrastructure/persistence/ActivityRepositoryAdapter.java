package org.acme.dashboard.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.acme.dashboard.application.model.Activity;
import org.acme.dashboard.application.port.output.ActivityRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Se arma con las tablas que ya existen, sin audit_logs (decisión del módulo 7). */
@ApplicationScoped
public class ActivityRepositoryAdapter implements ActivityRepository {

    private static final String SQL = """
            SELECT * FROM (
              SELECT 'PAYMENT' AS kind, p.id, p.obligation_id AS link_id, o.title, p.amount, p.currency, p.created_at AS at
                FROM payments p JOIN obligations o ON o.id = p.obligation_id
               WHERE p.user_id = :userId AND p.voided_at IS NULL
              UNION ALL
              SELECT 'INVOICE', i.id, i.id, s.name, i.total_amount, i.currency, i.created_at
                FROM invoices i JOIN services s ON s.id = i.service_id
               WHERE i.user_id = :userId
              UNION ALL
              SELECT 'STATEMENT', cs.id, cs.card_id, c.name, cs.total_amount, cs.currency, cs.created_at
                FROM card_statements cs JOIN cards c ON c.id = cs.card_id
               WHERE cs.user_id = :userId
              UNION ALL
              SELECT 'DOCUMENT', d.id, d.id, COALESCE(NULLIF(d.title, ''), d.original_filename), NULL, NULL, d.created_at
                FROM documents d
               WHERE d.user_id = :userId AND d.deleted_at IS NULL
            ) a ORDER BY at DESC LIMIT :limit
            """;

    @Inject
    EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<Activity> latest(UUID userId, int limit) {
        List<Object[]> rows = em.createNativeQuery(SQL).setParameter("userId", userId).setParameter("limit", limit).getResultList();
        return rows.stream().map(r -> new Activity((String) r[0], (UUID) r[1], (UUID) r[2], (String) r[3], (BigDecimal) r[4],
                r[5] == null ? null : r[5].toString().trim(), instant(r[6]))).toList();
    }

    private static Instant instant(Object value) {
        return value instanceof OffsetDateTime odt ? odt.toInstant() : (Instant) value;
    }
}
