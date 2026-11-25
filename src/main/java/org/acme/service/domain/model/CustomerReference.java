package org.acme.service.domain.model;

import java.util.Locale;

/** Nro. de socio / cliente / suministro. */
public final class CustomerReference {

    private CustomerReference() {
    }

    /**
     * Forma comparable: sin espacios, guiones ni puntos y en minúsculas, porque la factura
     * y el usuario lo escriben distinto ("10.234-5" y "102345"). null si no queda nada.
     */
    public static String normalize(String reference) {
        if (reference == null) {
            return null;
        }
        String normalized = reference.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
