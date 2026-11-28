package org.acme.calendar.infrastructure.rest;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.calendar.application.usecase.CalendarUseCase;
import org.acme.calendar.infrastructure.rest.dto.CalendarTokenResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Vencimientos en el calendario del usuario (módulo 19). */
@Path("/calendar")
@Tag(name = "Calendario")
public class CalendarResource {

    @Inject
    CalendarUseCase calendar;

    @Inject
    CurrentUser currentUser;

    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    @SecurityRequirement(name = "JWT Auth")
    @Operation(summary = "Token del link iCal", description = "La primera vez lo crea")
    public CalendarTokenResponse token() {
        return new CalendarTokenResponse(calendar.token(currentUser.id()));
    }

    @POST
    @Path("/regenerate")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    @SecurityRequirement(name = "JWT Auth")
    @Operation(summary = "Generar otro link", description = "El link anterior deja de funcionar")
    public CalendarTokenResponse regenerate() {
        return new CalendarTokenResponse(calendar.regenerate(currentUser.id()));
    }

    @GET
    @Path("/feeds/{token}.ics")
    @PermitAll
    @Produces("text/calendar; charset=utf-8")
    @Operation(summary = "Archivo iCal", description = "Público: el token es la clave. Los vencimientos sin pagar")
    public String feed(@PathParam("token") String token) {
        return calendar.feed(token);
    }
}
