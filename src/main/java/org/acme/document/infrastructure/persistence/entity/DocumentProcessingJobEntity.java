package org.acme.document.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.document.domain.model.ProcessingJobStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_processing_jobs")
@Getter
@Setter
public class DocumentProcessingJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProcessingJobStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "provider", length = 80)
    private String provider;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "started_at")
    private Instant startedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
