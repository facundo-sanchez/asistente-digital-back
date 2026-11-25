package org.acme.user.application.usecase;

import org.acme.shared.application.port.output.EmailSender;
import org.acme.shared.domain.model.EmailMessage;

import java.util.ArrayList;
import java.util.List;

/** Guarda los emails en lugar de enviarlos. */
public class RecordingEmailSender implements EmailSender {

    private final List<EmailMessage> sent = new ArrayList<>();

    public List<EmailMessage> sent() {
        return sent;
    }

    public EmailMessage last() {
        if (sent.isEmpty()) {
            throw new AssertionError("No se envió ningún email");
        }
        return sent.get(sent.size() - 1);
    }

    @Override
    public void send(EmailMessage message) {
        sent.add(message);
    }
}
