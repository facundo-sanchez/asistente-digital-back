package org.acme.integration.infrastructure.rest;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.integration.application.usecase.ConnectGmailUseCase;
import org.acme.integration.application.usecase.GmailSyncUseCase;
import org.acme.integration.infrastructure.rest.dto.AuthorizationUrlResponse;
import org.acme.integration.infrastructure.rest.dto.GmailAuthorizationRequest;
import org.acme.integration.infrastructure.rest.dto.GmailConnectRequest;
import org.acme.integration.infrastructure.rest.dto.IntegrationResponse;
import org.acme.integration.infrastructure.rest.dto.SyncResponse;
import org.acme.integration.infrastructure.rest.mapper.IntegrationRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/** Integraciones (CU-003, CU-013): por ahora Gmail, solo lectura. */
@Path("/integrations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Integraciones")
public class IntegrationResource {

    @Inject
    ConnectGmailUseCase gmail;

    @Inject
    GmailSyncUseCase sync;

    @Inject
    IntegrationRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Cuentas conectadas", description = "Sin las desconectadas")
    public List<IntegrationResponse> list() {
        return gmail.list(currentUser.id()).stream().map(mapper::toResponse).toList();
    }

    @POST
    @Path("/gmail/authorization")
    @Operation(summary = "URL de Google para dar el permiso de lectura de Gmail")
    public AuthorizationUrlResponse authorization(@Valid @NotNull GmailAuthorizationRequest request) {
        return new AuthorizationUrlResponse(gmail.authorizationUrl(request.state(), request.codeChallenge()).toString());
    }

    @POST
    @Path("/gmail")
    @Operation(summary = "Conectar Gmail con el código que devolvió Google")
    public IntegrationResponse connect(@Valid @NotNull GmailConnectRequest request) {
        return mapper.toResponse(gmail.connect(currentUser.id(), request.code(), request.codeVerifier(), request.syncFrom()));
    }

    @POST
    @Path("/{id}/sync")
    @Operation(summary = "Sincronizar ahora")
    public SyncResponse syncNow(@PathParam("id") UUID id) {
        int imported = sync.syncNow(currentUser.id(), id);
        IntegrationResponse integration = gmail.list(currentUser.id()).stream().filter(i -> i.getId().equals(id)).findFirst()
                .map(mapper::toResponse).orElse(null);
        return new SyncResponse(imported, integration);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Desconectar", description = "Revoca el permiso en Google y borra los tokens; lo importado queda (RN-011)")
    public void disconnect(@PathParam("id") UUID id) {
        gmail.disconnect(currentUser.id(), id);
    }
}
