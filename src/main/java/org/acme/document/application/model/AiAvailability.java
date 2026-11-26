package org.acme.document.application.model;

import org.acme.shared.domain.model.DocumentType;

import java.util.Set;

/** Si la lectura con IA está disponible y para qué tipos de documento. */
public record AiAvailability(boolean available, Set<DocumentType> documentTypes) {}
