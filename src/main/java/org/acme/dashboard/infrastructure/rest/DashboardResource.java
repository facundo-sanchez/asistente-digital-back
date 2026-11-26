package org.acme.dashboard.infrastructure.rest;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.dashboard.application.model.DashboardSpending;
import org.acme.dashboard.application.model.DashboardSummary;
import org.acme.dashboard.application.usecase.DashboardUseCase;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Los números del Inicio (CU-012). */
@Path("/dashboard")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Dashboard")
public class DashboardResource {

    @Inject
    DashboardUseCase dashboard;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Resumen del Inicio", description = "Mes actual en la zona del usuario; importes por moneda (no se suman ni convierten)")
    public DashboardSummary summary() {
        return dashboard.summary(currentUser.id());
    }

    @GET
    @Path("/spending")
    @Operation(summary = "Pagado por mes y por categoría", description = "Últimos 6 meses por moneda (con ceros) y el mes actual por categoría del servicio")
    public DashboardSpending spending() {
        return dashboard.spending(currentUser.id());
    }
}
