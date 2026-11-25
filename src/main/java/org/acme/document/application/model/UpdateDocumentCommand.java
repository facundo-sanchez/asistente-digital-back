package org.acme.document.application.model;

import org.acme.shared.domain.model.DocumentType;

import java.time.LocalDate;
import java.util.UUID;

/** Datos editables de un documento. serviceId y expiresOn en null los quitan. */
public record UpdateDocumentCommand(String title, DocumentType documentType, UUID serviceId, LocalDate expiresOn) {}
