package org.acme.card.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Categoría que el usuario eligió para un comercio; se aplica a consumos futuros (RN-019). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CardMerchantRule {
    private UUID id;
    private UUID userId;
    private String merchantName;
    private SpendingCategory category;
    private Instant createdAt;
    private Instant updatedAt;
}
