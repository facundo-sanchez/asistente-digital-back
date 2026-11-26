package org.acme.document.infrastructure.ai;

import org.acme.document.application.DocumentReadException;
import org.acme.document.application.model.DocumentInput;
import org.acme.document.application.model.ReadFailure;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Lo que no depende de llamar a Gemini: configuración y limpieza de la respuesta. */
class GeminiDocumentReaderTest {

    private final GeminiDocumentReader reader = new GeminiDocumentReader(null, Optional.of("clave"));

    @Test
    void sinClaveNoEstaDisponibleYNoLlamaAlModelo() {
        GeminiDocumentReader withoutKey = new GeminiDocumentReader(null, Optional.of(" "));

        assertFalse(withoutKey.available());
        DocumentReadException e = assertThrows(DocumentReadException.class,
                () -> withoutKey.readJson(DocumentInput.ofText("x"), "y"));
        assertEquals(ReadFailure.NOT_CONFIGURED, e.failure());
        assertFalse(e.retryable());
    }

    @Test
    void aceptaElJsonAunqueVengaEntreBacktickYRechazaLoQueNoEsJson() {
        assertEquals("{\"total\": 216050.90}", reader.validJson("```json\n{\"total\": 216050.90}\n```"));
        assertEquals("{\"a\":1}", reader.validJson(" {\"a\":1} "));

        DocumentReadException e = assertThrows(DocumentReadException.class, () -> reader.validJson("No encontré datos"));
        assertEquals(ReadFailure.INVALID_RESPONSE, e.failure());
        assertTrue(e.retryable());
    }
}
