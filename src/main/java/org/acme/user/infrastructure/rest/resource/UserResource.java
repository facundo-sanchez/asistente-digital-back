package org.acme.user.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.acme.user.application.usecase.PasswordUseCase;
import org.acme.user.application.usecase.ProfileUseCase;
import org.acme.user.infrastructure.rest.dto.AuthResponse;
import org.acme.user.infrastructure.rest.dto.ChangePasswordRequest;
import org.acme.user.infrastructure.rest.dto.PaymentWalletRequest;
import org.acme.user.infrastructure.rest.dto.UpdateProfileRequest;
import org.acme.user.infrastructure.rest.dto.UserResponse;
import org.acme.user.infrastructure.rest.mapper.UserRestMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Clock;

/** Perfil del usuario autenticado. El usuario sale del token, nunca de la URL. */
@Path("/users/me")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Usuario")
public class UserResource {

    @Inject
    ProfileUseCase profile;

    @Inject
    PasswordUseCase passwords;

    @Inject
    UserRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @Inject
    Clock clock;

    @GET
    @Operation(summary = "Consultar el perfil")
    public UserResponse get() {
        return mapper.toResponse(profile.get(currentUser.id()));
    }

    @PUT
    @Operation(summary = "Actualizar el perfil")
    public UserResponse update(@Valid @NotNull UpdateProfileRequest request) {
        return mapper.toResponse(profile.update(currentUser.id(), mapper.toCommand(request)));
    }

    @PUT
    @Path("/payment-wallet")
    @Operation(summary = "Elegir la billetera para pagar facturas")
    public UserResponse paymentWallet(@NotNull PaymentWalletRequest request) {
        return mapper.toResponse(profile.setPaymentWallet(currentUser.id(), request.paymentWallet()));
    }

    @PUT
    @Path("/password")
    @Operation(summary = "Cambiar la contraseña",
            description = "Cierra las demás sesiones y devuelve una sesión nueva")
    public AuthResponse changePassword(@Valid @NotNull ChangePasswordRequest request) {
        return mapper.toResponse(
                passwords.change(currentUser.id(), request.currentPassword(), request.newPassword()),
                clock.instant());
    }
}
