package org.acme.shared.infrastructure.persistence;

import java.util.Locale;

/**
 * Patrones para "lower(campo) like :valor escape '\'". El texto del usuario se busca
 * literal: % y _ no actúan como comodines.
 */
public final class LikePattern {

    /** Fragmento JPQL que acompaña al patrón. */
    public static final String ESCAPE = " escape '\\'";

    private LikePattern() {
    }

    /** "%texto%" en minúsculas, con los comodines del usuario escapados. */
    public static String contains(String text) {
        return "%" + escape(text.toLowerCase(Locale.ROOT)) + "%";
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
