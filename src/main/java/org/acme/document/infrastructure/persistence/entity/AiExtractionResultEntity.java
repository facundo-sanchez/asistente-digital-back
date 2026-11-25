package org.acme.document.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.shared.domain.model.DocumentType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_extraction_results")
@Getter
@Setter
public class AiExtractionResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_type", length = 50)
    private DocumentType detectedType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> extractedData = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "confidence_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Double> confidenceData = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 30)
    private ExtractionValidationStatus validationStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @PrePersist
    @PreUpdate
    void ensureDefaults() {
        if (extractedData == null) extractedData = new HashMap<>();
        if (confidenceData == null) confidenceData = new HashMap<>();
    }
}
