package org.acme.shared.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Evento del Transactional Outbox (SPEC §9.3). Uso interno, no se expone por REST. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    private UUID id;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private UUID userId;
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;
    private int attempts;
    private Instant availableAt;
    private Instant lockedUntil;
    private String lastError;
    private Instant createdAt;
    private Instant processedAt;
}
