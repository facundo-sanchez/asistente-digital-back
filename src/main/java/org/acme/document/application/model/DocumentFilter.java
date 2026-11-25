package org.acme.document.application.model;

import org.acme.shared.domain.model.DocumentType;

import java.util.UUID;

/**
 * Filtros del listado de documentos. Todos opcionales (null = sin filtro).
 *
 * @param text busca en el título y en el nombre del archivo, ya normalizado
 */
public record DocumentFilter(String text, DocumentType documentType, UUID serviceId) {}
