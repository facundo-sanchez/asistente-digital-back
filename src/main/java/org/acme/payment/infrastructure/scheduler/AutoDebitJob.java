package org.acme.payment.infrastructure.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.payment.application.usecase.AutoDebitUseCase;
import org.jboss.logging.Logger;

/** Cada hora: registra los débitos automáticos del día de cada usuario (los ya pagados no se repiten). */
@ApplicationScoped
public class AutoDebitJob {

    private static final Logger LOG = Logger.getLogger(AutoDebitJob.class);

    @Inject
    AutoDebitUseCase autoDebit;

    @Scheduled(cron = "{app.payments.auto-debit-cron:0 10 * * * ?}", delayed = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        try {
            int paid = autoDebit.payDue();
            if (paid > 0) {
                LOG.infof("Débitos automáticos registrados: %d", paid);
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "No se pudieron registrar los débitos automáticos");
        }
    }
}
