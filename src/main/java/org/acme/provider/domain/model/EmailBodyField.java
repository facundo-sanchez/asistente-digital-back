package org.acme.provider.domain.model;

/**
 * Cómo extraer un campo del cuerpo de un email.
 *
 * <p>Campo simple (groups null): el grupo 1 de la regex es el valor y format dice cómo
 * interpretarlo. Campo de filas (groups con valor): la regex se aplica todas las veces
 * que coincida (un email puede traer varias facturas, ej. Coopelectric) y cada grupo es
 * un dato de la fila.
 *
 * @param regex  expresión regular (java.util.regex). Se aplica sobre el asunto y el texto
 *               del cuerpo sin HTML, con los espacios normalizados (incluido U+00A0).
 * @param format formato del valor de un campo simple: "dd-MM-yyyy" o "dd/MM/yyyy" (fecha),
 *               "es-AR" (importe 1.234,56), "decimal" (importe 1234.56) o null (texto tal cual).
 * @param groups solo en campos de filas: nombre y formato de cada grupo, en orden, separados
 *               por coma; ej. "customerReference,dueDate:dd/MM/yyyy,totalAmount:decimal".
 */
public record EmailBodyField(String regex, String format, String groups) {

    /** Campo simple (un solo valor). */
    public EmailBodyField(String regex, String format) {
        this(regex, format, null);
    }

    public boolean isRows() {
        return groups != null && !groups.isBlank();
    }
}
