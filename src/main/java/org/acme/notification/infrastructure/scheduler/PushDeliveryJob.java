package org.acme.notification.infrastructure.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.notification.application.usecase.PushDeliveryUseCase;
import org.jboss.logging.Logger;

/** Cada minuto: manda las notificaciones push pendientes. */
@ApplicationScoped
public class PushDeliveryJob {

    private static final Logger LOG = Logger.getLogger(PushDeliveryJob.class);

    @Inject
    PushDeliveryUseCase push;

    @Scheduled(every = "{app.push.every:1m}", delayed = "45s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void run() {
        try {
            push.send();
        } catch (RuntimeException e) {
            LOG.errorf(e, "No se pudieron mandar las notificaciones push");
        }
    }
}
