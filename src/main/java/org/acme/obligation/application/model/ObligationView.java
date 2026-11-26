package org.acme.obligation.application.model;

import org.acme.obligation.domain.model.ObligationSummary;

/** Una obligación con su saldo, estado efectivo y el nombre de su servicio (null si no tiene). */
public record ObligationView(ObligationSummary summary, String serviceName) {}
