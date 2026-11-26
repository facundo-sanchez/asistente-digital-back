package org.acme.notification.infrastructure.rest;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.acme.notification.application.usecase.NotificationQueryUseCase;
import org.acme.notification.infrastructure.rest.dto.NotificationResponse;
import org.acme.notification.infrastructure.rest.dto.UnreadCountResponse;
import org.acme.notification.infrastructure.rest.mapper.NotificationRestMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.infrastructure.rest.dto.PageResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/** Avisos en la app (CU-007): últimos 30 días. */
@Path("/notifications")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Notificaciones")
public class NotificationResource {

    @Inject
    NotificationQueryUseCase notifications;

    @Inject
    NotificationRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Avisos de los últimos 30 días", description = "Los más nuevos primero; unread=true solo los no leídos")
    public PageResponse<NotificationResponse> list(@QueryParam("unread") @DefaultValue("false") boolean unread,
                                                 @QueryParam("page") @DefaultValue("0") int page,
                                                 @QueryParam("size") @DefaultValue("20") int size) {
        return PageResponse.from(notifications.list(currentUser.id(), unread, new PageQuery(page, size)), mapper::toResponse);
    }

    @GET
    @Path("/unread-count")
    @Operation(summary = "Cantidad de avisos sin leer")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(notifications.unreadCount(currentUser.id()));
    }

    @PATCH
    @Path("/{id}/read")
    @Operation(summary = "Marcar un aviso como leído")
    public NotificationResponse markRead(@PathParam("id") UUID id) {
        return mapper.toResponse(notifications.markRead(currentUser.id(), id));
    }

    @POST
    @Path("/read-all")
    @Operation(summary = "Marcar todos como leídos")
    public UnreadCountResponse markAllRead() {
        notifications.markAllRead(currentUser.id());
        return new UnreadCountResponse(0);
    }
}
