package org.acme.integration.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.integration.domain.model.ProcessedItemResult;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_processed_items")
@Getter
@Setter
public class IntegrationProcessedItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "integration_id", nullable = false)
    private UUID integrationId;

    @Column(name = "external_message_id", nullable = false)
    private String externalMessageId;

    @Column(name = "external_attachment_id", nullable = false)
    private String externalAttachmentId;

    @Column(name = "document_id")
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 30)
    private ProcessedItemResult result;

    @Column(name = "provider_id")
    private UUID providerId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "sender")
    private String sender;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "discard_reason", length = 100)
    private String discardReason;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    void ensureDefaults() {
        if (externalAttachmentId == null) externalAttachmentId = "";
    }
}
