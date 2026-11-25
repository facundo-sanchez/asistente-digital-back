package org.acme.user.infrastructure.rest.mapper;

import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.acme.user.application.model.AuthSession;
import org.acme.user.application.model.RegisterCommand;
import org.acme.user.application.model.UpdateProfileCommand;
import org.acme.user.domain.model.User;
import org.acme.user.infrastructure.rest.dto.AuthResponse;
import org.acme.user.infrastructure.rest.dto.RegisterRequest;
import org.acme.user.infrastructure.rest.dto.UpdateProfileRequest;
import org.acme.user.infrastructure.rest.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Duration;
import java.time.Instant;

@Mapper(config = MapStructConfig.class)
public interface UserRestMapper {

    @Mapping(target = "hasPassword", expression = "java(user.hasPassword())")
    UserResponse toResponse(User user);

    RegisterCommand toCommand(RegisterRequest request);

    UpdateProfileCommand toCommand(UpdateProfileRequest request);

    default AuthResponse toResponse(AuthSession session, Instant now) {
        return new AuthResponse(
                session.accessToken(),
                "Bearer",
                secondsUntil(session.accessTokenExpiresAt(), now),
                session.refreshToken(),
                secondsUntil(session.refreshTokenExpiresAt(), now),
                toResponse(session.user())
        );
    }

    private static long secondsUntil(Instant expiresAt, Instant now) {
        return Math.max(0, Duration.between(now, expiresAt).toSeconds());
    }
}
