package org.acme.card.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardStatement;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationStatus;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.obligation.domain.model.ObligationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Vencimientos de un resumen: uno por moneda con saldo (RN-018, no se suman ni se convierten).
 * Crea, actualiza o cancela cada uno según los importes del resumen.
 */
@ApplicationScoped
public class StatementObligations {

    static final String HAS_PAYMENTS = "Sus vencimientos tienen pagos registrados: anulalos antes de cambiar el resumen";
    static final String CANCEL_REASON = "Resumen anulado";
    static final String USD = "USD";

    private final ObligationRepository obligations;

    @Inject
    public StatementObligations(ObligationRepository obligations) {
        this.obligations = obligations;
    }

    /** Ajusta los dos vencimientos al resumen y deja sus ids en él. No guarda el resumen. */
    public void sync(UUID userId, Card card, CardStatement statement, Instant now) {
        statement.setObligationId(syncOne(userId, card, statement, statement.getObligationId(),
                statement.getTotalAmount(), card.getCurrency(), now));
        statement.setObligationUsdId(syncOne(userId, card, statement, statement.getObligationUsdId(),
                statement.getTotalAmountUsd(), USD, now));
    }

    /** Cancela los vencimientos del resumen (anulación). */
    public void cancelAll(UUID userId, CardStatement statement, Instant now) {
        for (UUID id : new UUID[]{statement.getObligationId(), statement.getObligationUsdId()}) {
            ObligationSummary summary = find(userId, id);
            if (summary != null && summary.obligation().getStatus() != ObligationStatus.CANCELLED) {
                summary.obligation().cancel(CANCEL_REASON, now);
                obligations.save(summary.obligation());
            }
        }
    }

    /** Con pagos en alguno de sus vencimientos, el resumen no se puede cambiar (RN-008). */
    public void requireNoPayments(UUID userId, CardStatement statement) {
        for (UUID id : new UUID[]{statement.getObligationId(), statement.getObligationUsdId()}) {
            ObligationSummary summary = find(userId, id);
            if (summary != null && summary.paidAmount().signum() > 0) {
                throw new ServiceException(HAS_PAYMENTS);
            }
        }
    }

    public ObligationSummary find(UUID userId, UUID obligationId) {
        return obligationId == null ? null : obligations.findByIdForUser(userId, obligationId).orElse(null);
    }

    /** Devuelve el id del vencimiento vigente, o null si el importe quedó en cero. */
    private UUID syncOne(UUID userId, Card card, CardStatement statement, UUID currentId,
                         BigDecimal amount, String currency, Instant now) {
        ObligationSummary current = find(userId, currentId);
        boolean hasBalance = amount != null && amount.signum() > 0;
        if (!hasBalance) {
            if (current != null) {
                current.obligation().cancel(CANCEL_REASON, now);
                obligations.save(current.obligation());
            }
            return null;
        }
        Obligation obligation = current == null ? Obligation.builder().userId(userId).build() : current.obligation();
        obligation.setServiceId(card.getServiceId());
        obligation.setTitle(title(card, statement, currency));
        obligation.setObligationType(ObligationType.CREDIT_CARD_PAYMENT);
        obligation.setAmount(amount);
        obligation.setCurrency(currency);
        obligation.setDueDate(statement.getDueDate());
        return obligations.save(obligation).obligation().getId();
    }

    /** «Visa Galicia · resumen 2026-12», y «… · dólares» para el de USD. El mes es el del cierre (o del vencimiento). */
    static String title(Card card, CardStatement statement, String currency) {
        YearMonth period = YearMonth.from(statement.getClosingDate() != null ? statement.getClosingDate() : statement.getDueDate());
        return card.getName() + " · resumen " + period + (USD.equals(currency) ? " · dólares" : "");
    }
}
