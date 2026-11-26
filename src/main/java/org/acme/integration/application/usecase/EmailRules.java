package org.acme.integration.application.usecase;

import org.acme.integration.application.model.EmailRule;
import org.acme.integration.application.model.GmailAttachment;
import org.acme.integration.application.model.GmailMessage;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cómo se aplica una regla de proveedor a un email (ver V11). */
final class EmailRules {

    /** Lo que la IA puede leer como adjunto. */
    private static final Set<String> READABLE = Set.of("pdf", "jpg", "jpeg", "png", "webp", "xls", "xlsx");

    private EmailRules() {
    }

    /** La primera regla (ya ordenadas por prioridad) cuyo remitente y asunto coinciden. */
    static Optional<EmailRule> match(List<EmailRule> rules, GmailMessage message) {
        return rules.stream().filter(r -> senderMatches(r, message.sender()) && find(r.subjectRegex(), message.subject())).findFirst();
    }

    /** Exactamente la dirección de la regla, o un remitente del dominio del proveedor (o de un subdominio). */
    static boolean senderMatches(EmailRule rule, String sender) {
        if (sender == null) return false;
        String address = sender.toLowerCase(Locale.ROOT);
        if (rule.senderAddress() != null && address.equals(rule.senderAddress().toLowerCase(Locale.ROOT))) return true;
        String domain = address.substring(address.indexOf('@') + 1);
        return rule.emailDomains().stream().map(d -> d.toLowerCase(Locale.ROOT)).anyMatch(d -> domain.equals(d) || domain.endsWith("." + d));
    }

    /** Adjuntos que la IA puede leer y que cumplen el nombre de la regla (si tiene). */
    static List<GmailAttachment> readableAttachments(EmailRule rule, GmailMessage message) {
        return message.attachments().stream()
                .filter(a -> READABLE.contains(extension(a.filename())))
                .filter(a -> find(rule.attachmentNameRegex(), a.filename()))
                .toList();
    }

    /** Grupo 1 de la regex del nro. de cliente sobre asunto + cuerpo. */
    static Optional<String> customerReference(EmailRule rule, GmailMessage message) {
        if (rule.customerReferenceRegex() == null) return Optional.empty();
        Matcher m = Pattern.compile(rule.customerReferenceRegex()).matcher(message.subject() + "\n" + message.bodyText());
        return m.find() && m.groupCount() >= 1 ? Optional.ofNullable(m.group(1)) : Optional.empty();
    }

    private static boolean find(String regex, String text) {
        return regex == null || (text != null && Pattern.compile(regex).matcher(text).find());
    }

    private static String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
