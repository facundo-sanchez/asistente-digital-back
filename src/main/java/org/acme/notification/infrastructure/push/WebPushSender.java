package org.acme.notification.infrastructure.push;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.acme.notification.application.PushSettings;
import org.acme.notification.application.model.PushGoneException;
import org.acme.notification.application.port.output.PushSender;
import org.acme.notification.domain.model.PushSubscription;
import org.acme.shared.application.AppSettings;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;

/** Web Push con claves VAPID (librería web-push). Sin claves configuradas, push no está disponible. */
@ApplicationScoped
public class WebPushSender implements PushSender {

    @Inject
    PushSettings keys;

    @Inject
    AppSettings settings;

    private PushService service;

    @PostConstruct
    void init() {
        if (!available()) {
            return;
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            // El "subject" identifica a quien manda: la URL pública de la app
            service = new PushService(keys.vapidPublicKey().get(), keys.vapidPrivateKey().get(), settings.frontendUrl());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Las claves VAPID no son válidas", e);
        }
    }

    @Override
    public boolean available() {
        return keys.vapidPublicKey().filter(k -> !k.isBlank()).isPresent() && keys.vapidPrivateKey().filter(k -> !k.isBlank()).isPresent();
    }

    @Override
    public String publicKey() {
        return keys.vapidPublicKey().orElse("");
    }

    @Override
    public void send(PushSubscription subscription, String payload) {
        HttpResponse response;
        try {
            Notification notification = new Notification(subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(),
                    payload.getBytes(StandardCharsets.UTF_8));
            response = service.send(notification);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar la notificación push: " + e.getClass().getSimpleName(), e);
        }
        int status = response.getStatusLine().getStatusCode();
        if (status == 404 || status == 410) {
            throw new PushGoneException("La suscripción ya no existe (" + status + ")");
        }
        if (status >= 400) {
            throw new IllegalStateException("El servicio de push respondió " + status);
        }
    }
}
