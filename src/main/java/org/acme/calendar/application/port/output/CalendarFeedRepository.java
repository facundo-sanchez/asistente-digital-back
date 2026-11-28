package org.acme.calendar.application.port.output;

import java.util.Optional;
import java.util.UUID;

/** El token del link iCal de cada usuario. */
public interface CalendarFeedRepository {

    Optional<String> findToken(UUID userId);

    Optional<UUID> findUser(String token);

    /** Crea o reemplaza el token del usuario. */
    void save(UUID userId, String token);
}
