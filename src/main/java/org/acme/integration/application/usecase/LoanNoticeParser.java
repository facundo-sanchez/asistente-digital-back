package org.acme.integration.application.usecase;

import org.acme.integration.application.model.LoanNotice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lee la frase fija del aviso de cuota de BBVA con un patrón (sin IA, no gasta cuota de Gemini). */
public final class LoanNoticeParser {

    private static final Pattern NOTICE = Pattern.compile(
            "(?i)la cuota\\s+(\\d+)\\s+de su pr[eé]stamo\\s+.*?\\s+(\\S+),\\s+por\\s+(ars|usd|u\\$s)\\s+([0-9.]*[0-9],[0-9]{2}|[0-9]+),\\s+vence el\\s+([0-9]{2}/[0-9]{2}/[0-9]{4})");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LoanNoticeParser() {
    }

    public static Optional<LoanNotice> parse(String text) {
        Matcher m = NOTICE.matcher(text == null ? "" : text.replace('\u00a0', ' '));
        if (!m.find()) {
            return Optional.empty();
        }
        String currency = m.group(3).toUpperCase(Locale.ROOT).startsWith("ARS") ? "ARS" : "USD";
        BigDecimal amount = new BigDecimal(m.group(4).replace(".", "").replace(",", "."));
        return Optional.of(new LoanNotice(Integer.parseInt(m.group(1)), m.group(2), currency, amount, LocalDate.parse(m.group(5), DATE)));
    }
}
