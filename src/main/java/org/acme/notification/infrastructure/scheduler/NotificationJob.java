package org.acme.notification.infrastructure.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.notification.application.usecase.GenerateNotificationsUseCase;
import org.jboss.logging.Logger;

/** Cada hora: a cada usuario le salen sus avisos cuando en su zona ya son las 9 (los repetidos no se crean). */
@ApplicationScoped
public class NotificationJob {

    private static final Logger LOG = Logger.getLogger(NotificationJob.class);

    @Inject
    GenerateNotificationsUseCase generator;

    @Scheduled(cron = "{app.notifications.cron:0 5 * * * ?}", delayed = "40s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        try {
            int created = generator.generate();
            if (created > 0) {
                LOG.infof("Avisos creados: %d", created);
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "No se pudieron generar los avisos");
        }
    }
}
