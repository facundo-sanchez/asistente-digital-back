package org.acme.user.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.acme.user.application.usecase.*;
import org.acme.user.infrastructure.rest.dto.*;
import org.acme.user.infrastructure.rest.mapper.UserRestMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Clock;

/** Autenticación: registro, login, refresh, logout, verificación de email y recupero de contraseña. */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth")
public class AuthResource {

    @Inject
    RegisterUserUseCase registerUser;

    @Inject
    AuthenticationUseCase authentication;

    @Inject
    EmailVerificationUseCase emailVerification;

    @Inject
    PasswordUseCase passwords;

    @Inject
    UserRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @Inject
    Clock clock;

    @POST
    @Path("/register")
    @PermitAll
    @Operation(summary = "Crear cuenta", description = "Deja la sesión iniciada y envía el email de verificación")
    public Response register(@Valid @NotNull RegisterRequest request) {
        AuthResponse body = mapper.toResponse(registerUser.register(mapper.toCommand(request)), clock.instant());
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Iniciar sesión con email y contraseña")
    public AuthResponse login(@Valid @NotNull LoginRequest request) {
        return mapper.toResponse(authentication.login(request.email(), request.password()), clock.instant());
    }

    @POST
    @Path("/refresh")
    @PermitAll
    @Operation(summary = "Renovar la sesión", description = "El refresh token es de un solo uso: se devuelve uno nuevo")
    public AuthResponse refresh(@Valid @NotNull RefreshTokenRequest request) {
        return mapper.toResponse(authentication.refresh(request.refreshToken()), clock.instant());
    }

    @POST
    @Path("/logout")
    @PermitAll
    @Operation(summary = "Cerrar sesión", description = "Invalida el refresh token")
    public Response logout(@Valid @NotNull RefreshTokenRequest request) {
        authentication.logout(request.refreshToken());
        return Response.noContent().build();
    }

    @POST
    @Path("/verify-email")
    @PermitAll
    @Operation(summary = "Confirmar el email con el token recibido")
    public Response verifyEmail(@Valid @NotNull TokenRequest request) {
        emailVerification.verify(request.token());
        return Response.noContent().build();
    }

    @POST
    @Path("/resend-verification")
    @Authenticated
    @SecurityRequirement(name = "JWT Auth")
    @Operation(summary = "Reenviar el email de verificación")
    public Response resendVerification() {
        emailVerification.resend(currentUser.id());
        return Response.noContent().build();
    }

    @POST
    @Path("/forgot-password")
    @PermitAll
    @Operation(summary = "Pedir el enlace para restablecer la contraseña",
            description = "Responde 202 siempre, exista o no la cuenta")
    public Response forgotPassword(@Valid @NotNull ForgotPasswordRequest request) {
        passwords.forgot(request.email());
        return Response.accepted().build();
    }

    @POST
    @Path("/reset-password")
    @PermitAll
    @Operation(summary = "Definir una contraseña nueva con el token recibido",
            description = "Cierra todas las sesiones abiertas")
    public Response resetPassword(@Valid @NotNull ResetPasswordRequest request) {
        passwords.reset(request.token(), request.newPassword());
        return Response.noContent().build();
    }
}
