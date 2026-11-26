package org.acme.document.application.port.output;

import org.acme.document.application.model.DocumentInput;

/**
 * Lee un documento con IA y devuelve los datos pedidos como JSON (texto). Las instrucciones
 * describen el formato esperado; validar ese JSON le toca a quien lo pide.
 * Lanza {@link org.acme.document.application.DocumentReadException} si no puede.
 */
public interface DocumentReader {

    /** false si no hay IA configurada: la app sigue funcionando con la carga manual. */
    boolean available();

    String readJson(DocumentInput input, String instructions);
}
