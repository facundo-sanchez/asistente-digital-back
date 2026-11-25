package org.acme.assistant.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Conversación con el asistente IA (CU-008). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    private UUID id;
    private UUID userId;
    private String title;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public void delete(Instant now) {
        if (deletedAt == null) {
            deletedAt = now;
        }
    }
}
