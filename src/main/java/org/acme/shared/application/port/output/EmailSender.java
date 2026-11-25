package org.acme.shared.application.port.output;

import org.acme.shared.domain.model.EmailMessage;

/**
 * Puerto de salida para enviar emails. Adaptadores: log (desarrollo) y SMTP.
 * El adaptador activo lo elige {@code EmailSenderProducer} según app.email.delivery.
 */
public interface EmailSender {
    void send(EmailMessage message);
}
