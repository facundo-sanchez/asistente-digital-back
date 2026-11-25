package org.acme.document.application;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** Configuración de documentos (prefijo "app.documents"). */
@ConfigMapping(prefix = "app.documents")
public interface DocumentSettings {

    /** Carpeta donde se guardan los archivos (DOCUMENTS_DIR). */
    @WithDefault("./data/documentos")
    String dir();

    /** Tamaño máximo de un archivo, en bytes (10 MB). */
    @WithDefault("10485760")
    long maxSize();
}
