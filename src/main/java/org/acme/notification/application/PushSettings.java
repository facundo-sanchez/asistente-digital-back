package org.acme.notification.application;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

/** Claves VAPID de las notificaciones push (VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY). Sin ellas, push no está disponible. */
@ConfigMapping(prefix = "app.push")
public interface PushSettings {

    Optional<String> vapidPublicKey();

    Optional<String> vapidPrivateKey();
}
