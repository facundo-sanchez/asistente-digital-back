package org.acme.integration.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Mensaje / adjunto ya procesado. Garantiza idempotencia y alimenta "detectado en tu correo". */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationProcessedItem {
    private UUID id;
    private UUID integrationId;
    private String externalMessageId;
    /** "" = el mensaje en sí, sin adjunto. */
    @Builder.Default
    private String externalAttachmentId = "";
    private UUID documentId;
    private ProcessedItemResult result;
    private UUID providerId;
    private UUID ruleId;
    private UUID serviceId;
    private String sender;
    private String subject;
    private Instant receivedAt;
    /** Ej. NOT_MATCHED, NO_ATTACHMENT, UNKNOWN_CUSTOMER. */
    private String discardReason;
    private Instant processedAt;
}
