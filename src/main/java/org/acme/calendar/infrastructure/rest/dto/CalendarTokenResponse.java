package org.acme.calendar.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** El token del link iCal; el front arma el link público con su propio dominio. */
@Schema(name = "CalendarTokenResponse")
public record CalendarTokenResponse(String token) {
}
