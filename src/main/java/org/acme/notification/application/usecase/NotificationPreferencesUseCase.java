package org.acme.notification.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.notification.application.model.NotificationSettings;
import org.acme.notification.application.port.output.NotificationPreferenceRepository;
import org.acme.notification.domain.model.NotificationPreference;
import org.acme.notification.domain.model.NotificationType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Qué avisos quiere el usuario, por qué canal y con cuántos días de anticipación (CU-007, RN-009). */
@ApplicationScoped
public class NotificationPreferencesUseCase {

    private final NotificationPreferenceRepository preferences;

    @Inject
    public NotificationPreferencesUseCase(NotificationPreferenceRepository preferences) {
        this.preferences = preferences;
    }

    public NotificationSettings settings(UUID userId) {
        return NotificationSettings.of(userId, preferences.findForUser(userId));
    }

    /** Guarda las que llegan (solo canales y tipos que se usan; con dos iguales vale la última) y devuelve las completas. */
    public NotificationSettings replace(UUID userId, List<NotificationPreference> requested) {
        Map<String, NotificationPreference> unique = new LinkedHashMap<>();
        for (NotificationPreference p : requested) {
            boolean used = NotificationSettings.CHANNELS.contains(p.getChannel())
                    && (p.getNotificationType() == NotificationType.ALL || NotificationSettings.TYPES.contains(p.getNotificationType()));
            if (used) {
                unique.put(p.getChannel() + ":" + p.getNotificationType(), p.toBuilder().id(null).userId(userId)
                        .daysBefore(new ArrayList<>(p.getDaysBefore().stream().distinct().sorted().toList())).build());
            }
        }
        preferences.replaceForUser(userId, List.copyOf(unique.values()));
        return settings(userId);
    }
}
