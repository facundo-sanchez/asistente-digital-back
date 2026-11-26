package org.acme.notification.infrastructure.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.notification.application.usecase.EmailDigestUseCase;
import org.jboss.logging.Logger;

/** Cada pocos minutos manda los avisos por email pendientes (y los reintentos que ya tocan). */
@ApplicationScoped
public class EmailDigestJob {

    private static final Logger LOG = Logger.getLogger(EmailDigestJob.class);

    @Inject
    EmailDigestUseCase digest;

    @Scheduled(every = "{app.notifications.email-every:5m}", delayed = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        try {
            int sent = digest.send();
            if (sent > 0) {
                LOG.infof("Emails de avisos enviados: %d", sent);
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "No se pudieron enviar los emails de avisos");
        }
    }
}
