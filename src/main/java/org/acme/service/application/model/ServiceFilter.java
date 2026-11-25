package org.acme.service.application.model;

import org.acme.shared.domain.model.ServiceCategory;

/**
 * Filtros del listado de servicios. Todos opcionales (null = sin filtro).
 *
 * @param text     parte del nombre o del nro. de socio, ya normalizado
 * @param category categoría exacta
 * @param active   solo activos (true) o solo inactivos (false)
 */
public record ServiceFilter(String text, ServiceCategory category, Boolean active) {}
