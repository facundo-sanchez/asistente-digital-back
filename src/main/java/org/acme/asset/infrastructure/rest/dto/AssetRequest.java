package org.acme.asset.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.acme.asset.domain.model.AssetType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Map;

@Schema(name = "AssetRequest")
public record AssetRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String name,

        @NotNull(message = "El tipo de bien es obligatorio")
        AssetType assetType,

        String description,

        @PastOrPresent(message = "La fecha de compra no puede ser futura")
        LocalDate purchaseDate,

        Map<String, Object> metadata
) {}
