package org.acme.notification.application.port.output;

import org.acme.notification.domain.model.NotificationPreference;

import java.util.List;
import java.util.UUID;

public interface NotificationPreferenceRepository {

    List<NotificationPreference> findForUser(UUID userId);

    /** Reemplaza todas las preferencias del usuario por estas. */
    void replaceForUser(UUID userId, List<NotificationPreference> preferences);
}
