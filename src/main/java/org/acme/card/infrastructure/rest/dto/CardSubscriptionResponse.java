package org.acme.card.infrastructure.rest.dto;

import org.acme.card.domain.model.SpendingCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Un comercio que se cobra todos los meses. increase: % sobre el mes anterior, solo desde 10 %. */
@Schema(name = "CardSubscriptionResponse")
public record CardSubscriptionResponse(UUID cardId, String cardName, @Schema(example = "NETFLIX.COM") String merchant, SpendingCategory category,
                                       String currency, BigDecimal lastAmount, LocalDate lastDate, BigDecimal previousAmount,
                                       LocalDate since, int months, @Schema(example = "25") Integer increase) {
}
