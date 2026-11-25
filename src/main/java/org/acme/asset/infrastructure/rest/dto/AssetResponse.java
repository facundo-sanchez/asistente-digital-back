package org.acme.asset.infrastructure.rest.dto;

import org.acme.asset.domain.model.AssetType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Schema(name = "AssetResponse")
public record AssetResponse(
        UUID id,
        @Schema(example = "Auto") String name,
        AssetType assetType,
        String description,
        LocalDate purchaseDate,
        Map<String, Object> metadata,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
