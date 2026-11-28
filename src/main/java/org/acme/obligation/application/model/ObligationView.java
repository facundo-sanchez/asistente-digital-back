package org.acme.obligation.application.model;

import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.shared.domain.model.ServiceCategory;

/**
 * Una obligación con su saldo, estado efectivo, y el nombre y la categoría de su servicio (null si
 * no tiene). La categoría da el color del servicio en las listas; serviceAutoDebit: el servicio se debita solo.
 */
public record ObligationView(ObligationSummary summary, String serviceName, ServiceCategory serviceCategory, boolean serviceAutoDebit) {

    /** Sin categoría: alcanza en las respuestas de alta y edición. */
    public ObligationView(ObligationSummary summary, String serviceName) {
        this(summary, serviceName, null, false);
    }
}
