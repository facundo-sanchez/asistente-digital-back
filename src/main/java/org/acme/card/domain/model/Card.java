package org.acme.card.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Tarjeta de crédito (CU-015). Pertenece a un servicio CREDIT_CARD del emisor. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    private UUID id;
    private UUID userId;
    private UUID serviceId;
    private String name;
    private CardBrand brand;
    /** Últimos 4 dígitos de la tarjeta. */
    private String lastFour;
    /** Últimos 4 dígitos del número de cuenta (algunos emisores, como BBVA, informan la cuenta en el email). */
    private String accountLastFour;
    private String holderName;
    private boolean additional;
    private Integer closingDay;
    private Integer dueDay;
    private BigDecimal creditLimit;
    @Builder.Default
    private String currency = "ARS";
    @Builder.Default
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;
}
