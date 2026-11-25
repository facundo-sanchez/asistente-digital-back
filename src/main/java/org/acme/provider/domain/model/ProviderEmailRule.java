package org.acme.provider.domain.model;

import lombok.*;
import org.acme.shared.domain.model.DocumentType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Regla para reconocer la factura de un proveedor en un email. Uso interno. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProviderEmailRule {
    private UUID id;
    private UUID providerId;
    private String senderAddress;
    private String subjectRegex;
    private String attachmentNameRegex;
    @Builder.Default
    private boolean requiresAttachment = true;
    @Builder.Default
    private DocumentType documentType = DocumentType.INVOICE;
    private String customerReferenceRegex;
    private String barcodeFormat;
    @Builder.Default
    private EmailExtractionMode extractionMode = EmailExtractionMode.ATTACHMENT;
    /** Campos a extraer del cuerpo cuando extractionMode = EMAIL_BODY (ej. dueDate, totalAmount). */
    @Builder.Default
    private Map<String, EmailBodyField> bodyFields = new HashMap<>();
    @Builder.Default
    private int priority = 100;
    @Builder.Default
    private boolean active = true;
    private Instant verifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
