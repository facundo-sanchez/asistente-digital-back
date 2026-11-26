package org.acme.document.application.model;

/**
 * Lo que se le da a leer a la IA: el texto de una planilla (Excel convertido a tabla) o el
 * archivo tal cual (PDF o imagen). Exactamente uno de los dos viene cargado.
 */
public record DocumentInput(String text, String mimeType, byte[] content) {

    public static DocumentInput ofText(String text) {
        return new DocumentInput(text, null, null);
    }

    public static DocumentInput ofFile(String mimeType, byte[] content) {
        return new DocumentInput(null, mimeType, content);
    }

    public boolean isText() {
        return text != null;
    }
}
