package org.acme.shared.infrastructure.email;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;

import java.util.ArrayList;
import java.util.List;

/** Mailer de prueba: guarda los emails en memoria o simula un SMTP caído. */
public class RecordingMailer implements Mailer {

    private final List<Mail> sent = new ArrayList<>();
    private RuntimeException failure;

    @Override
    public void send(Mail... mails) {
        if (failure != null) {
            throw failure;
        }
        sent.addAll(List.of(mails));
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public List<Mail> sent() {
        return sent;
    }
}
