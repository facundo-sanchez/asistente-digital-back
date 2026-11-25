package org.acme.card.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import org.acme.card.domain.model.SpendingCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** PATCH /card-transactions/{id}/category */
@Schema(name = "UpdateTransactionCategoryRequest")
public record UpdateTransactionCategoryRequest(
        @NotNull(message = "La categoría es obligatoria")
        SpendingCategory category,

        @Schema(description = "true = recordar esta categoría para el comercio (crea o actualiza la regla)")
        Boolean applyToMerchant
) {}
