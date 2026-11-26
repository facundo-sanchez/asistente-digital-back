package org.acme.integration.application.model;

import org.acme.provider.domain.model.EmailExtractionMode;
import org.acme.shared.domain.model.DocumentType;

import java.util.List;
import java.util.UUID;

/**
 * Regla de un proveedor para reconocer sus emails, con los dominios del proveedor. Un remitente coincide si es
 * exactamente senderAddress o si su dominio es uno de emailDomains (o un subdominio).
 */
public record EmailRule(UUID ruleId, UUID providerId, String senderAddress, List<String> emailDomains, String subjectRegex,
                        String attachmentNameRegex, DocumentType documentType, EmailExtractionMode extractionMode,
                        String customerReferenceRegex) {
}
