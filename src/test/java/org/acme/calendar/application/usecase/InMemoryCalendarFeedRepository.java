package org.acme.calendar.application.usecase;

import org.acme.calendar.application.port.output.CalendarFeedRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryCalendarFeedRepository implements CalendarFeedRepository {

    private final Map<UUID, String> tokens = new HashMap<>();

    @Override
    public Optional<String> findToken(UUID userId) {
        return Optional.ofNullable(tokens.get(userId));
    }

    @Override
    public Optional<UUID> findUser(String token) {
        return tokens.entrySet().stream().filter(e -> e.getValue().equals(token)).map(Map.Entry::getKey).findFirst();
    }

    @Override
    public void save(UUID userId, String token) {
        tokens.put(userId, token);
    }
}
