package org.acme.card.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Un comercio que se cobra todos los meses en una tarjeta. increase es el % sobre el mes anterior (null si subió menos del 10 %). */
public record CardSubscription(UUID cardId, String cardName, String merchant, SpendingCategory category, String currency,
                               BigDecimal lastAmount, LocalDate lastDate, BigDecimal previousAmount, LocalDate since, int months,
                               Integer increase) {
}
