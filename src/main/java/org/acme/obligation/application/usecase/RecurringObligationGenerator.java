package org.acme.obligation.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.application.port.output.RecurringRuleRepository;
import org.acme.obligation.domain.model.RecurringRule;

import java.time.LocalDate;

/**
 * Crea los vencimientos de una regla cuyo período empieza pronto (LEAD_DAYS). Si el servidor
 * estuvo apagado, se pone al día: crea también los períodos que quedaron atrás.
 */
@ApplicationScoped
public class RecurringObligationGenerator {

    /** Cuántos días antes del vencimiento aparece el período siguiente. */
    public static final int LEAD_DAYS = 20;
    /** Tope por regla y corrida: una fecha mal cargada (años atrás) no genera cientos de vencimientos. */
    static final int MAX_PER_RUN = 24;

    private final ObligationRepository obligations;
    private final RecurringRuleRepository rules;

    @Inject
    public RecurringObligationGenerator(ObligationRepository obligations, RecurringRuleRepository rules) {
        this.obligations = obligations;
        this.rules = rules;
    }

    /** Devuelve cuántos vencimientos creó. Cada regla va en su propia transacción. */
    @Transactional
    public int generate(RecurringRule rule, LocalDate today) {
        int created = 0;
        LocalDate limit = today.plusDays(LEAD_DAYS);
        while (rule.isActive() && !rule.getNextExpectedDate().isAfter(limit) && created < MAX_PER_RUN) {
            createCurrent(rule);
            created++;
        }
        if (created > 0) {
            rules.save(rule);
        }
        return created;
    }

    /** Crea el vencimiento del período pendiente (si no existe ya) y avanza la regla. No guarda la regla. */
    void createCurrent(RecurringRule rule) {
        if (!obligations.existsForRecurrence(rule.getId(), rule.getNextExpectedDate())) {
            obligations.save(rule.nextObligation());
        }
        rule.advance();
    }
}
