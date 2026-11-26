package org.acme.obligation.application.model;

import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.shared.domain.model.ServiceCategory;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Filtros del listado. null (o statuses vacío) = sin filtro.
 * category es la del servicio asociado; from y to acotan el vencimiento (inclusive).
 */
public record ObligationFilter(
        String text,
        Set<EffectiveObligationStatus> statuses,
        UUID serviceId,
        ServiceCategory category,
        LocalDate from,
        LocalDate to
) {}
