package org.acme.dashboard.application.model;

import org.acme.shared.domain.model.DocumentType;

import java.time.LocalDate;
import java.util.UUID;

/** Un documento (póliza, garantía...) que vence pronto. */
public record ExpiringDocument(UUID id, String title, DocumentType documentType, LocalDate expiresOn) {
}
