package org.acme.assistant.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AssistantMessage {
    private UUID id;
    private UUID conversationId;
    private MessageRole role;
    private String content;
    /** Herramientas usadas, referencias a registros, consumo de tokens. */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    private Instant createdAt;
}
