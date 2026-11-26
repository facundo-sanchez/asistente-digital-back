package org.acme.integration.application.port.output;

import org.acme.integration.domain.model.Integration;

/** Avisarle al usuario que tiene que volver a conectar una cuenta (Google revocó o venció el permiso). */
public interface IntegrationAlerts {

    void reconnectNeeded(Integration integration);
}
