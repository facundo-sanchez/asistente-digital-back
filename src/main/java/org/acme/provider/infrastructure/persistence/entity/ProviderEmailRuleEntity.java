package org.acme.provider.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.provider.domain.model.EmailExtractionMode;
import org.acme.shared.domain.model.DocumentType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "provider_email_rules")
@Getter
@Setter
public class ProviderEmailRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "sender_address")
    private String senderAddress;

    @Column(name = "subject_regex", length = 500)
    private String subjectRegex;

    @Column(name = "attachment_name_regex", length = 500)
    private String attachmentNameRegex;

    @Column(name = "requires_attachment", nullable = false)
    private boolean requiresAttachment;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "customer_reference_regex", length = 500)
    private String customerReferenceRegex;

    @Column(name = "barcode_format", length = 50)
    private String barcodeFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_mode", nullable = false, length = 20)
    private EmailExtractionMode extractionMode;

    /** Campo -> {"regex": "...", "format": "..."}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "body_fields", nullable = false, columnDefinition = "jsonb")
    private Map<String, Map<String, String>> bodyFields = new HashMap<>();

    // SMALLINT en la base: sin esto Hibernate valida la columna como INTEGER

    @JdbcTypeCode(SqlTypes.SMALLINT)

    @Column(name = "priority", nullable = false, columnDefinition = "smallint")
    private int priority;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void ensureDefaults() {
        if (bodyFields == null) bodyFields = new HashMap<>();
    }
}
