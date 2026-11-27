package org.acme.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import org.acme.user.domain.model.EmailCheckInterval;

public record EmailCheckIntervalRequest(@NotNull(message = "Elegí cada cuánto revisar") EmailCheckInterval interval) {}
