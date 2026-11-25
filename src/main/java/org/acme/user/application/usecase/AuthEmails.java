package org.acme.user.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.shared.application.AppSettings;
import org.acme.shared.domain.model.EmailMessage;
import org.acme.user.domain.model.User;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Textos de los emails de autenticación. */
@ApplicationScoped
public class AuthEmails {

    private final AppSettings settings;

    @Inject
    public AuthEmails(AppSettings settings) {
        this.settings = settings;
    }

    public EmailMessage verification(User user, String token) {
        String link = link("/verify-email", token);
        return new EmailMessage(user.getEmail(), "Confirmá tu email",
                "Hola " + user.getName() + ",\n\n"
                        + "Para confirmar tu email ingresá a:\n" + link + "\n\n"
                        + "El enlace vence en " + hours(settings.auth().emailVerificationTtl()) + " horas.\n"
                        + "Si no creaste una cuenta, ignorá este mensaje.");
    }

    public EmailMessage passwordReset(User user, String token) {
        String link = link("/reset-password", token);
        return new EmailMessage(user.getEmail(), "Restablecer tu contraseña",
                "Hola " + user.getName() + ",\n\n"
                        + "Recibimos un pedido para restablecer tu contraseña. Ingresá a:\n" + link + "\n\n"
                        + "El enlace vence en " + minutes(settings.auth().passwordResetTtl()) + " minutos y se puede usar una sola vez.\n"
                        + "Si no lo pediste, ignorá este mensaje: tu contraseña no cambia.");
    }

    private String link(String path, String token) {
        String base = settings.frontendUrl().replaceAll("/+$", "");
        return base + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private static long hours(Duration d) {
        return d.toHours();
    }

    private static long minutes(Duration d) {
        return d.toMinutes();
    }
}
