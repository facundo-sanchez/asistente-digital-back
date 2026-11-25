package org.acme.shared.infrastructure.email;

import io.quarkus.logging.Log;
import org.acme.shared.application.port.output.EmailSender;
import org.acme.shared.domain.model.EmailMessage;

/**
 * Adaptador de desarrollo: escribe el email en el log en lugar de enviarlo.
 * Los emails de verificación y reseteo incluyen un enlace con token de un solo uso,
 * por eso este adaptador NO debe usarse en producción.
 */
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        Log.infof("[EMAIL DEV] Para: %s | Asunto: %s%n%s", message.to(), message.subject(), message.body());
    }
}
