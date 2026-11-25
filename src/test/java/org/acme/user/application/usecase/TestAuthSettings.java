package org.acme.user.application.usecase;

import org.acme.shared.application.AuthSettings;

import java.time.Duration;

/** Configuración de autenticación para tests (mismos plazos que producción). */
public class TestAuthSettings implements AuthSettings {

    @Override
    public String issuer() {
        return "test";
    }

    @Override
    public String audience() {
        return "test-api";
    }

    @Override
    public Duration accessTokenTtl() {
        return Duration.ofMinutes(15);
    }

    @Override
    public Duration refreshTokenTtl() {
        return Duration.ofDays(30);
    }

    @Override
    public Duration emailVerificationTtl() {
        return Duration.ofDays(2);
    }

    @Override
    public Duration passwordResetTtl() {
        return Duration.ofHours(1);
    }

    @Override
    public int bcryptCost() {
        return 4;
    }
}
