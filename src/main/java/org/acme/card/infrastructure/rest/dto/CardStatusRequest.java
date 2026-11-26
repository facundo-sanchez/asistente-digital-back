package org.acme.card.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Activar o desactivar una tarjeta. */
@Schema(name = "CardStatusRequest")
public record CardStatusRequest(@NotNull(message = "El estado es obligatorio") Boolean active) {}
