package org.acme.document.application.port.output;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Dónde se guardan los archivos de los documentos. La clave la arma el sistema
 * ({userId}/{uuid}), nunca el usuario.
 */
public interface FileStorage {

    void save(String key, Path source) throws IOException;

    InputStream open(String key) throws IOException;

    /** No falla si el archivo no existe. */
    void delete(String key) throws IOException;
}
