package org.acme.shared.application;

import io.smallrye.config.WithDefault;

/** Configuración de emails (prefijo "app.email"). Grupo de {@link AppSettings}. */
public interface EmailSettings {

    /** Adaptador de envío. Los datos del servidor SMTP se configuran en quarkus.mailer.*. */
    @WithDefault("log")
    EmailDelivery delivery();
}
