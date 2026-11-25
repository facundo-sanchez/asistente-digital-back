package org.acme.shared.application;

import io.smallrye.config.WithDefault;

import java.time.Duration;

/** Configuración de autenticación (prefijo "app.auth"). Grupo de {@link AppSettings}. */
public interface AuthSettings {

    String issuer();

    String audience();

    @WithDefault("PT15M")
    Duration accessTokenTtl();

    @WithDefault("P30D")
    Duration refreshTokenTtl();

    @WithDefault("P2D")
    Duration emailVerificationTtl();

    @WithDefault("PT1H")
    Duration passwordResetTtl();

    /** Costo de bcrypt (2^n iteraciones). */
    @WithDefault("12")
    int bcryptCost();
}
