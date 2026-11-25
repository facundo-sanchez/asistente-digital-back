package org.acme.document.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Seguimiento del procesamiento asíncrono de un documento. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingJob {
    private UUID id;
    private UUID documentId;
    private UUID userId;
    @Builder.Default
    private ProcessingJobStatus status = ProcessingJobStatus.PENDING;
    private int attempts;
    @Builder.Default
    private int maxAttempts = 5;
    private String provider;
    private String errorCode;
    /** Mensaje técnico, sin contenido documental. Uso interno. */
    private String errorMessage;
    private Instant nextRetryAt;
    private Instant lockedUntil;
    private Instant startedAt;
    private Instant createdAt;
    private Instant completedAt;
}
