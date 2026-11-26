package org.acme.card.application.model;

import org.acme.card.domain.model.CardStatement;
import org.acme.obligation.domain.model.ObligationSummary;

/** Un resumen con el nombre de su tarjeta y sus vencimientos en pesos y en dólares (null si no tiene). */
public record CardStatementView(CardStatement statement, String cardName, ObligationSummary local, ObligationSummary usd) {}
