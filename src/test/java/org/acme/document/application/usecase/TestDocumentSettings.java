package org.acme.document.application.usecase;

import org.acme.document.application.DocumentSettings;

/** Configuración de prueba: máximo de 1 KB para probar el límite sin archivos grandes. */
public class TestDocumentSettings implements DocumentSettings {

    static final long MAX_SIZE = 1024;

    @Override
    public String dir() {
        return "no-se-usa";
    }

    @Override
    public long maxSize() {
        return MAX_SIZE;
    }
}
