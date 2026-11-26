package org.acme.document.application;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;

/** Límites de la lectura con IA. */
@ConfigMapping(prefix = "app.ai")
public interface AiSettings {

    /** Lecturas que puede pedir un usuario por día (evita costos inesperados). */
    @WithDefault("50")
    int dailyLimit();

    /** Cuánto queda bloqueado un trabajo mientras se procesa; si el proceso se cae, otro lo retoma después. */
    @WithDefault("10m")
    Duration lock();
}
