package org.acme.shared.application;

import io.smallrye.config.ConfigMapping;

/** Configuración de la aplicación (prefijo "app" en application.properties). */
@ConfigMapping(prefix = "app")
public interface AppSettings {

    /** URL pública del frontend, para armar los links de los emails. */
    String frontendUrl();

    AuthSettings auth();

    EmailSettings email();

    GoogleSettings google();
}
