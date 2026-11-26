package org.acme.integration.infrastructure.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.integration.application.usecase.GmailSyncUseCase;
import org.jboss.logging.Logger;

/** Cada 15 minutos trae de Gmail las facturas y resúmenes nuevos de todas las cuentas conectadas. */
@ApplicationScoped
public class GmailSyncJob {

    private static final Logger LOG = Logger.getLogger(GmailSyncJob.class);

    @Inject
    GmailSyncUseCase sync;

    @Scheduled(every = "{app.integrations.sync-every:15m}", delayed = "90s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        try {
            sync.syncAll();
        } catch (RuntimeException e) {
            LOG.errorf(e, "No se pudo sincronizar Gmail");
        }
    }
}
