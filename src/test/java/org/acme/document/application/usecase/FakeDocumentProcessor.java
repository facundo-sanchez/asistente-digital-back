package org.acme.document.application.usecase;

import org.acme.document.application.port.output.DocumentProcessor;
import org.acme.document.domain.model.Document;
import org.acme.shared.domain.model.DocumentType;

import java.util.Set;
import java.util.UUID;

/** Procesador de resúmenes de prueba: falla con el error que se le indique o termina bien. */
public class FakeDocumentProcessor implements DocumentProcessor {

    RuntimeException failWith;
    int calls;

    @Override
    public Set<DocumentType> supports() {
        return Set.of(DocumentType.CARD_STATEMENT);
    }

    @Override
    public void process(Document document, UUID jobId) {
        calls++;
        if (failWith != null) {
            throw failWith;
        }
    }
}
