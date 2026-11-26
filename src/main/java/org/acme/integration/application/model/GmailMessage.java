package org.acme.integration.application.model;

import java.time.Instant;
import java.util.List;

/**
 * Un email ya leído: remitente (solo la dirección), asunto, cuándo llegó, el texto del cuerpo (sin HTML) y sus adjuntos.
 */
public record GmailMessage(String id, String sender, String subject, Instant receivedAt, String bodyText, List<GmailAttachment> attachments) {
}
