package org.acme.notification.infrastructure.rest;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.notification.application.usecase.PushSubscriptionUseCase;
import org.acme.notification.domain.model.PushSubscription;
import org.acme.notification.infrastructure.rest.dto.PushEndpointRequest;
import org.acme.notification.infrastructure.rest.dto.PushPublicKeyResponse;
import org.acme.notification.infrastructure.rest.dto.PushSubscriptionRequest;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Notificaciones push de este dispositivo (módulo 15). */
@Path("/push-subscriptions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Avisos")
public class PushSubscriptionResource {

    @Inject
    PushSubscriptionUseCase push;

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/public-key")
    @Operation(summary = "Clave pública VAPID", description = "503 si push no está configurado")
    public PushPublicKeyResponse publicKey() {
        return new PushPublicKeyResponse(push.publicKey());
    }

    @POST
    @Operation(summary = "Activar push en este dispositivo")
    public void subscribe(@Valid @NotNull PushSubscriptionRequest request) {
        push.subscribe(currentUser.id(), PushSubscription.builder()
                .endpoint(request.endpoint()).p256dh(request.keys().p256dh()).auth(request.keys().auth()).userAgent(request.userAgent()).build());
    }

    @POST
    @Path("/remove")
    @Operation(summary = "Desactivar push en este dispositivo")
    public void unsubscribe(@Valid @NotNull PushEndpointRequest request) {
        push.unsubscribe(currentUser.id(), request.endpoint());
    }
}
