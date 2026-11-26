package org.acme.integration.infrastructure.notification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.integration.application.port.output.IntegrationAlerts;
import org.acme.integration.domain.model.Integration;
import org.acme.notification.application.usecase.GenerateNotificationsUseCase;

@ApplicationScoped
public class IntegrationAlertsAdapter implements IntegrationAlerts {

    @Inject
    GenerateNotificationsUseCase notifications;

    @Override
    public void reconnectNeeded(Integration integration) {
        notifications.integrationDisconnected(integration.getUserId(), integration.getId(), integration.getExternalAccountId());
    }
}
