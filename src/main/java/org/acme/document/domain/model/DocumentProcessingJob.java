package org.acme.document.domain.model;

import lombok.*;

import java.time.Duration;
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

    /** Esperas entre reintentos: 1, 2, 4, 8… minutos (hasta maxAttempts). */
    public static Duration backoff(int attempts) {
        return Duration.ofMinutes(1L << Math.max(0, Math.min(attempts - 1, 10)));
    }

    /** Lo toma un procesador: cuenta el intento y lo bloquea hasta lockedUntil (si se cae, otro lo retoma después). */
    public void start(Instant now, Duration lock) {
        status = ProcessingJobStatus.PROCESSING;
        attempts++;
        lockedUntil = now.plus(lock);
        nextRetryAt = null;
        if (startedAt == null) {
            startedAt = now;
        }
    }

    /** La IA leyó el documento: queda esperando que el usuario revise la propuesta. */
    public void succeed(Instant now) {
        status = ProcessingJobStatus.REQUIRES_REVIEW;
        lockedUntil = null;
        errorCode = null;
        errorMessage = null;
        completedAt = now;
    }

    /** Falló: se reintenta más tarde si se puede y quedan intentos; si no, queda DEAD. */
    public void fail(Instant now, String code, String message, boolean retryable) {
        errorCode = code;
        errorMessage = message;
        lockedUntil = null;
        if (retryable && attempts < maxAttempts) {
            status = ProcessingJobStatus.FAILED;
            nextRetryAt = now.plus(backoff(attempts));
        } else {
            status = ProcessingJobStatus.DEAD;
            completedAt = now;
        }
    }

    /** true mientras espera o se está procesando: no se pide otro para el mismo documento. */
    public boolean isActive() {
        return status == ProcessingJobStatus.PENDING || status == ProcessingJobStatus.PROCESSING || status == ProcessingJobStatus.FAILED;
    }

    /** El estado que muestra el documento. */
    public ProcessingStatus documentStatus() {
        return switch (status) {
            case PENDING, FAILED -> ProcessingStatus.PENDING;
            case PROCESSING -> ProcessingStatus.PROCESSING;
            case REQUIRES_REVIEW -> ProcessingStatus.REQUIRES_REVIEW;
            case COMPLETED -> ProcessingStatus.COMPLETED;
            case DEAD -> ProcessingStatus.FAILED;
        };
    }
}
