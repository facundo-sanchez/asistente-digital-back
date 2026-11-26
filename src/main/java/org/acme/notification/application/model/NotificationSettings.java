package org.acme.notification.application.model;

import org.acme.notification.domain.model.NotificationChannel;
import org.acme.notification.domain.model.NotificationPreference;
import org.acme.notification.domain.model.NotificationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Las preferencias de un usuario (RN-009), completas: por canal una fila ALL (si el canal está activo y con qué días
 * de anticipación) y una por tipo (si ese aviso está activo). Lo que el usuario no guardó toma el valor por defecto.
 */
public record NotificationSettings(List<NotificationPreference> preferences) {

    /** Los canales que se pueden elegir; PUSH queda para más adelante. */
    public static final List<NotificationChannel> CHANNELS = List.of(NotificationChannel.WEB, NotificationChannel.EMAIL);
    /** Los avisos que se generan hoy. */
    public static final List<NotificationType> TYPES = List.of(NotificationType.OBLIGATION_DUE_SOON, NotificationType.OBLIGATION_OVERDUE,
            NotificationType.DOCUMENT_EXPIRING, NotificationType.DOCUMENT_DETECTED, NotificationType.INTEGRATION_DISCONNECTED);

    /** Las guardadas sobre los valores por defecto. */
    public static NotificationSettings of(UUID userId, List<NotificationPreference> saved) {
        List<NotificationPreference> all = new ArrayList<>();
        for (NotificationChannel channel : CHANNELS) {
            all.add(saved(saved, channel, NotificationType.ALL).orElse(defaultFor(userId, channel, NotificationType.ALL)));
            for (NotificationType type : TYPES) {
                all.add(saved(saved, channel, type).orElse(defaultFor(userId, channel, type)));
            }
        }
        return new NotificationSettings(all);
    }

    public boolean wants(NotificationChannel channel, NotificationType type) {
        return find(channel, NotificationType.ALL).isEnabled() && find(channel, type).isEnabled();
    }

    /** Días de anticipación del canal, de menor a mayor y sin repetidos. */
    public List<Integer> days(NotificationChannel channel) {
        return find(channel, NotificationType.ALL).getDaysBefore().stream().distinct().sorted().toList();
    }

    private NotificationPreference find(NotificationChannel channel, NotificationType type) {
        return preferences.stream().filter(p -> p.getChannel() == channel && p.getNotificationType() == type).findFirst().orElseThrow();
    }

    private static Optional<NotificationPreference> saved(List<NotificationPreference> saved, NotificationChannel channel,
                                                                  NotificationType type) {
        return saved.stream().filter(p -> p.getChannel() == channel && p.getNotificationType() == type).findFirst();
    }

    /** En la app: todo. Por email: vencimientos, vencidos y documentos, a 3, 1 y 0 días (la lectura de la IA, no). */
    private static NotificationPreference defaultFor(UUID userId, NotificationChannel channel, NotificationType type) {
        boolean email = channel == NotificationChannel.EMAIL;
        return NotificationPreference.builder().userId(userId).channel(channel).notificationType(type)
                .enabled(!(email && type == NotificationType.DOCUMENT_DETECTED))
                .daysBefore(new ArrayList<>(email ? List.of(0, 1, 3) : List.of(0, 1, 3, 7))).build();
    }
}
