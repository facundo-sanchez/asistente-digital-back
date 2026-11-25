package org.acme.provider.application.model;

import org.acme.shared.domain.model.ServiceCategory;

/**
 * Filtros del listado de proveedores. Ambos opcionales (null = sin filtro).
 *
 * @param text     parte del nombre, sin distinguir mayúsculas (ya normalizado: sin espacios de más)
 * @param category categoría exacta
 */
public record ProviderFilter(String text, ServiceCategory category) {}
