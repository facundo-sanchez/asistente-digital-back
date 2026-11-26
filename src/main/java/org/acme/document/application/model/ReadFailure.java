package org.acme.document.application.model;

/** Por qué no se pudo leer un documento con la IA. Se guarda como código, nunca con el contenido. */
public enum ReadFailure {
    /** No hay clave de Gemini configurada. */
    NOT_CONFIGURED,
    /** El servicio tardó demasiado o no respondió: se puede reintentar. */
    UNAVAILABLE,
    /** Respondió algo que no es el JSON pedido. */
    INVALID_RESPONSE,
    /** El archivo no se pudo preparar (planilla dañada, tipo no admitido). */
    UNREADABLE_FILE
}
