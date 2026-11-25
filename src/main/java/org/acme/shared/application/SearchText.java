package org.acme.shared.application;

/** Texto de búsqueda que escribe el usuario en un listado. */
public final class SearchText {

    private SearchText() {
    }

    /** Sin espacios de más; null o vacío equivale a no filtrar (devuelve null). */
    public static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }
}
