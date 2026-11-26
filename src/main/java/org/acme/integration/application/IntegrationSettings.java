package org.acme.integration.application;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Integraciones (prefijo "app.integrations"). Sin key, conectar Gmail queda deshabilitado.
 * Las URLs solo se cambian en pruebas.
 */
@ConfigMapping(prefix = "app.integrations")
public interface IntegrationSettings {

    /** Clave AES de 256 bits en base64 para cifrar los tokens (INTEGRATIONS_KEY). */
    Optional<String> key();

    @WithDefault("https://gmail.googleapis.com/gmail/v1/users/me")
    String gmailApiUri();

    @WithDefault("https://oauth2.googleapis.com/revoke")
    String revokeUri();

    /** Mensajes como máximo por sincronización de cada cuenta. */
    @WithDefault("50")
    int maxMessagesPerSync();
}
