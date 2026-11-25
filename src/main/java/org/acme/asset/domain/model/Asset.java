package org.acme.asset.domain.model;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bien personal (CU-010). */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    private UUID id;
    private UUID userId;
    private String name;
    private AssetType assetType;
    private String description;
    private LocalDate purchaseDate;
    /** Datos específicos: patente, dirección, modelo... */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    @Builder.Default
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;
}
