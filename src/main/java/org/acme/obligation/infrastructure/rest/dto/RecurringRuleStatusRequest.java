package org.acme.obligation.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Activar o desactivar una repetición. */
@Schema(name = "RecurringRuleStatusRequest")
public record RecurringRuleStatusRequest(@NotNull(message = "Indicá si queda activa") Boolean active) {}
