package org.acme.notification.infrastructure.rest;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.notification.application.model.NotificationSettings;
import org.acme.notification.application.usecase.NotificationPreferencesUseCase;
import org.acme.notification.infrastructure.rest.dto.NotificationPreferenceRequest;
import org.acme.notification.infrastructure.rest.dto.NotificationPreferenceResponse;
import org.acme.notification.infrastructure.rest.mapper.NotificationRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/** Preferencias de avisos (CU-007, RN-009): por canal (WEB, EMAIL) una fila ALL y una por tipo de aviso. */
@Path("/notification-preferences")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Notificaciones")
public class NotificationPreferenceResource {

    @Inject
    NotificationPreferencesUseCase preferences;

    @Inject
    NotificationRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Preferencias completas", description = "Lo que el usuario no guardó viene con el valor por defecto")
    public List<NotificationPreferenceResponse> get() {
        return toResponse(preferences.settings(currentUser.id()));
    }

    @PUT
    @Operation(summary = "Reemplazar las preferencias", description = "Se ignoran PUSH y los tipos que no se generan")
    public List<NotificationPreferenceResponse> replace(@NotNull @Valid List<@NotNull @Valid NotificationPreferenceRequest> request) {
        return toResponse(preferences.replace(currentUser.id(), request.stream().map(mapper::toModel).toList()));
    }

    private List<NotificationPreferenceResponse> toResponse(NotificationSettings settings) {
        return settings.preferences().stream().map(mapper::toResponse).toList();
    }
}
