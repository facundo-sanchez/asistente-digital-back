package org.acme.user.domain.model;

import java.time.Duration;

/** Cada cuánto se revisa el correo del usuario (módulo 13.6). DAILY y MANUAL no tienen intervalo fijo. */
public enum EmailCheckInterval {
    EVERY_15_MINUTES(Duration.ofMinutes(15)),
    HOURLY(Duration.ofHours(1)),
    EVERY_6_HOURS(Duration.ofHours(6)),
    DAILY(Duration.ofDays(1)),
    MANUAL(null);

    private final Duration every;

    EmailCheckInterval(Duration every) {
        this.every = every;
    }

    public Duration every() {
        return every;
    }
}
