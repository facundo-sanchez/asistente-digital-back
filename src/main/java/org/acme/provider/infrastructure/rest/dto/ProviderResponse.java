package org.acme.provider.infrastructure.rest.dto;

import org.acme.shared.domain.model.ServiceCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "ProviderResponse")
public record ProviderResponse(
        UUID id,
        @Schema(example = "Coopelectric") String name,
        @Schema(example = "COOPELECTRIC") String code,
        ServiceCategory category,
        @Schema(example = "https://www.coopelectric.com.ar") String website,
        @Schema(description = "true si lo creó el usuario (no pertenece al catálogo)") boolean custom,
        boolean active
) {}
