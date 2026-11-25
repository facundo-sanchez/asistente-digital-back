package org.acme.shared.infrastructure.email;

import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.acme.shared.application.port.output.EmailSender;
import org.acme.shared.domain.model.EmailMessage;

/**
 * Envía los emails por SMTP con quarkus-mailer. El servidor, el usuario y el
 * remitente se configuran en quarkus.mailer.* (variables SMTP_*).
 *
 * <p>Si el envío falla, se registra el error y no se propaga: un SMTP caído no debe
 * impedir registrarse ni pedir el recupero de contraseña. El usuario puede pedir
 * el reenvío. El log no incluye el cuerpo porque contiene un token de un solo uso.
 */
public class SmtpEmailSender implements EmailSender {

    private final Mailer mailer;

    public SmtpEmailSender(Mailer mailer) {
        this.mailer = mailer;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            mailer.send(Mail.withText(message.to(), message.subject(), message.body()));
        } catch (RuntimeException e) {
            Log.errorf(e, "No se pudo enviar el email \"%s\" a %s", message.subject(), message.to());
        }
    }
}
