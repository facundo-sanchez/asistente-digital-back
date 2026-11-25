package org.acme.shared.infrastructure.email;

import io.quarkus.mailer.Mail;
import org.acme.shared.domain.model.EmailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SmtpEmailSenderTest {

    private static final EmailMessage MESSAGE =
            new EmailMessage("ana@mail.com", "Confirmá tu email", "Ingresá a http://localhost:3000/verify-email?token=abc");

    private RecordingMailer mailer;
    private SmtpEmailSender sender;

    @BeforeEach
    void setUp() {
        mailer = new RecordingMailer();
        sender = new SmtpEmailSender(mailer);
    }

    @Test
    void enviaElEmailComoTextoPlano() {
        sender.send(MESSAGE);

        assertEquals(1, mailer.sent().size());
        Mail mail = mailer.sent().getFirst();
        assertEquals(List.of("ana@mail.com"), mail.getTo());
        assertEquals("Confirmá tu email", mail.getSubject());
        assertEquals(MESSAGE.body(), mail.getText());
    }

    @Test
    void unSmtpCaidoNoInterrumpeElFlujo() {
        mailer.failWith(new IllegalStateException("Connection refused"));

        assertDoesNotThrow(() -> sender.send(MESSAGE));
    }
}
