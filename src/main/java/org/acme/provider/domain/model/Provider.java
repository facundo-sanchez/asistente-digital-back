package org.acme.provider.domain.model;

import lombok.*;
import org.acme.shared.domain.model.ServiceCategory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Proveedor del catálogo global (userId null) o propio de un usuario. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Provider {
    private UUID id;
    private UUID userId;
    private String name;
    private String code;
    private ServiceCategory category;
    private String website;
    /** Dominios remitentes usados para buscar facturas en Gmail. */
    @Builder.Default
    private List<String> emailDomains = new ArrayList<>();
    @Builder.Default
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isCustom() {
        return userId != null;
    }
}
