package org.acme.document.infrastructure.rest.dto;

import org.acme.shared.domain.model.DocumentType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;

/** Si se puede pedir la lectura con IA y para qué tipos de documento. */
@Schema(name = "AiAvailabilityResponse")
public record AiAvailabilityResponse(boolean available, Set<DocumentType> documentTypes) {}
