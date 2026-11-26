package org.acme.document.application.usecase;

import org.acme.document.application.model.DocumentInput;
import org.acme.document.application.port.output.DocumentReader;

/** IA falsa: solo dice si está configurada. Los procesadores de prueba no la llaman. */
public class FakeDocumentReader implements DocumentReader {

    boolean available = true;

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public String readJson(DocumentInput input, String instructions) {
        return "{}";
    }
}
