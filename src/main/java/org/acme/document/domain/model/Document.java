package org.acme.document.domain.model;

import lombok.*;
import org.acme.shared.domain.model.DocumentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Metadatos de un documento. El archivo vive en Object Storage (storageKey). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private UUID id;
    private UUID userId;
    private UUID serviceId;
    private UUID assetId;
    private UUID obligationId;
    @Builder.Default
    private DocumentType documentType = DocumentType.OTHER;
    private String title;
    private String originalFilename;
    /** Ruta interna en el storage. Nunca se expone por REST. */
    private String storageKey;
    private String mimeType;
    private long fileSize;
    /** SHA-256 en hex. Se usa para detectar archivos duplicados. */
    private String checksum;
    @Builder.Default
    private DocumentSource source = DocumentSource.MANUAL;
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;
    private LocalDate expiresOn;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void delete(Instant now) {
        if (deletedAt == null) {
            deletedAt = now;
        }
    }
}
