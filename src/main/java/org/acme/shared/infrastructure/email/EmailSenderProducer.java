package org.acme.shared.infrastructure.email;

import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.acme.shared.application.AppSettings;
import org.acme.shared.application.port.output.EmailSender;

/** Elige el adaptador de envío de emails según app.email.delivery (EMAIL_DELIVERY). */
@ApplicationScoped
public class EmailSenderProducer {

    @Produces
    @ApplicationScoped
    public EmailSender emailSender(AppSettings settings, Mailer mailer) {
        return switch (settings.email().delivery()) {
            case SMTP -> new SmtpEmailSender(mailer);
            case LOG -> new LoggingEmailSender();
        };
    }
}
