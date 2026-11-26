package org.acme.integration.infrastructure.google;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** El HTML de un email como texto legible (para la IA y las reglas): sin estilos ni etiquetas, con saltos de línea. */
final class EmailHtml {

    private static final Map<String, String> ENTITIES = Map.ofEntries(
            Map.entry("nbsp", " "), Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"), Map.entry("quot", "\""),
            Map.entry("apos", "'"), Map.entry("aacute", "á"), Map.entry("eacute", "é"), Map.entry("iacute", "í"), Map.entry("oacute", "ó"),
            Map.entry("uacute", "ú"), Map.entry("ntilde", "ñ"), Map.entry("Aacute", "Á"), Map.entry("Eacute", "É"), Map.entry("Iacute", "Í"),
            Map.entry("Oacute", "Ó"), Map.entry("Uacute", "Ú"), Map.entry("Ntilde", "Ñ"), Map.entry("deg", "°"), Map.entry("iquest", "¿"),
            Map.entry("iexcl", "¡"));
    private static final Pattern ENTITY = Pattern.compile("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);");

    private EmailHtml() {
    }

    static String toText(String html) {
        String text = html.replaceAll("(?is)<(style|script|head)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>|</(p|div|tr|li|h[1-6]|table)>", "\n")
                .replaceAll("(?i)</t[dh]>", " ")
                .replaceAll("(?s)<[^>]+>", "");
        text = decodeEntities(text).replace(' ', ' ');
        return text.replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll(" *\\n *", "\n").replaceAll("\\n{2,}", "\n").trim();
    }

    private static String decodeEntities(String text) {
        Matcher m = ENTITY.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String value;
            if (name.startsWith("#x") || name.startsWith("#X")) {
                value = new String(Character.toChars(Integer.parseInt(name.substring(2), 16)));
            } else if (name.startsWith("#")) {
                value = new String(Character.toChars(Integer.parseInt(name.substring(1))));
            } else {
                value = ENTITIES.getOrDefault(name, m.group());
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }
}
