package org.acme.shared.infrastructure.email;

import org.acme.shared.application.EmailDelivery;
import org.acme.shared.application.port.output.EmailSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EmailSenderProducerTest {

    private final EmailSenderProducer producer = new EmailSenderProducer();

    @Test
    void conDeliveryLogEscribeEnElLog() {
        assertInstanceOf(LoggingEmailSender.class, senderFor(EmailDelivery.LOG));
    }

    @Test
    void conDeliverySmtpEnviaPorSmtp() {
        assertInstanceOf(SmtpEmailSender.class, senderFor(EmailDelivery.SMTP));
    }

    private EmailSender senderFor(EmailDelivery delivery) {
        return producer.emailSender(new EmailTestSettings(delivery), new RecordingMailer());
    }
}
