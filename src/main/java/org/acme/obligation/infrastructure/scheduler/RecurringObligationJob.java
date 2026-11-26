package org.acme.obligation.infrastructure.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.obligation.application.usecase.ManageRecurringRuleUseCase;
import org.acme.obligation.application.usecase.RecurringObligationGenerator;
import org.acme.obligation.domain.model.RecurringRule;
import org.jboss.logging.Logger;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Todos los días crea los vencimientos de las reglas cuyo próximo período empieza pronto.
 * También corre al arrancar, así un servidor apagado unos días se pone al día solo.
 */
@ApplicationScoped
public class RecurringObligationJob {

    private static final Logger LOG = Logger.getLogger(RecurringObligationJob.class);

    @Inject
    ManageRecurringRuleUseCase recurringRules;

    @Inject
    RecurringObligationGenerator generator;

    @Inject
    Clock clock;

    // shortcut: "hoy" es el del servidor, no el de cada usuario (RN-012); con LEAD_DAYS de margen no cambia el resultado
    @Scheduled(cron = "{app.recurrence.cron:0 0 5 * * ?}", delayed = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        LocalDate today = LocalDate.now(clock);
        int created = 0;
        for (RecurringRule rule : recurringRules.dueRules()) {
            try {
                created += generator.generate(rule, today);
            } catch (RuntimeException e) {
                // Una regla con problemas no frena a las demás
                LOG.errorf(e, "No se pudieron generar los vencimientos de la regla %s", rule.getId());
            }
        }
        if (created > 0) {
            LOG.infof("Vencimientos recurrentes creados: %d", created);
        }
    }
}
