package org.acme.calendar.application.usecase;

import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Arma el archivo iCalendar (RFC 5545): un evento de todo el día por vencimiento. */
final class IcsWriter {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final String CRLF = "\r\n";

    private IcsWriter() {
    }

    static String write(List<ObligationSummary> items, String appUrl, Instant now) {
        StringBuilder ics = new StringBuilder();
        line(ics, "BEGIN:VCALENDAR");
        line(ics, "VERSION:2.0");
        line(ics, "PRODID:-//Asistente de Vida Digital//Vencimientos//ES");
        line(ics, "CALSCALE:GREGORIAN");
        line(ics, "METHOD:PUBLISH");
        line(ics, "X-WR-CALNAME:Vencimientos");
        // Sugerencia a los calendarios: volver a leerlo cada 6 horas (Google decide su propio ritmo)
        line(ics, "REFRESH-INTERVAL;VALUE=DURATION:PT6H");
        line(ics, "X-PUBLISHED-TTL:PT6H");
        for (ObligationSummary item : items) {
            Obligation o = item.obligation();
            line(ics, "BEGIN:VEVENT");
            line(ics, "UID:" + o.getId() + "@asistente-digital");
            line(ics, "DTSTAMP:" + STAMP.format(now));
            line(ics, "DTSTART;VALUE=DATE:" + DAY.format(o.getDueDate()));
            line(ics, "DTEND;VALUE=DATE:" + DAY.format(o.getDueDate().plusDays(1)));
            line(ics, "SUMMARY:" + escape(summary(o.getTitle(), item.balance(), o.getCurrency())));
            line(ics, "URL:" + appUrl + "/obligations/" + o.getId());
            line(ics, "DESCRIPTION:" + escape("Abrilo en la app: " + appUrl + "/obligations/" + o.getId()));
            line(ics, "TRANSP:TRANSPARENT");
            line(ics, "END:VEVENT");
        }
        line(ics, "END:VCALENDAR");
        return ics.toString();
    }

    /** "Gas · $ 12.345,67"; sin importe, solo el título. */
    static String summary(String title, BigDecimal amount, String currency) {
        if (amount == null || currency == null) {
            return title;
        }
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-AR")));
        return title + " · " + ("ARS".equals(currency) ? "$" : "US$".equals(currency) || "USD".equals(currency) ? "US$" : currency) + " "
                + format.format(amount);
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n");
    }

    /** Líneas de hasta 75 bytes; las más largas siguen en la próxima con un espacio adelante. */
    private static void line(StringBuilder ics, String content) {
        int bytes = 0;
        for (int i = 0; i < content.length(); ) {
            int cp = content.codePointAt(i);
            int size = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + size > 75) {
                ics.append(CRLF).append(' ');
                bytes = 1;
            }
            ics.appendCodePoint(cp);
            bytes += size;
            i += Character.charCount(cp);
        }
        ics.append(CRLF);
    }
}
