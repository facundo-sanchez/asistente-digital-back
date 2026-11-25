package org.acme.provider.domain.model;

/**
 * Cómo extraer un campo del cuerpo de un email.
 *
 * @param regex  expresión regular (java.util.regex); el grupo 1 es el valor.
 *               Se aplica sobre el texto sin HTML y con los espacios normalizados
 *               (incluido el espacio duro U+00A0).
 * @param format cómo interpretar el valor: "dd-MM-yyyy" (fecha), "es-AR" (importe 1.234,56)
 *               o null (texto tal cual).
 */
public record EmailBodyField(String regex, String format) {}
