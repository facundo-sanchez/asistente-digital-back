package org.acme.card.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.acme.document.application.DocumentReadException;
import org.acme.document.application.model.ReadFailure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** La respuesta de la IA se acepta solo si tiene la forma de un resumen. */
class CardStatementProcessorTest {

    private final CardStatementProcessor processor =
            new CardStatementProcessor(null, null, null, new ObjectMapper().registerModule(new JavaTimeModule()), "test");

    @Test
    void aceptaElObjetoOElPrimeroDeUnaListaYRechazaLoQueNoEsUnResumen() {
        assertEquals("2026-11-11", processor.parse("{\"dueDate\":\"2026-11-11\",\"totalAmount\":216050.90}").get("dueDate"));
        assertEquals("BBVA", processor.parse("[{\"bank\":\"BBVA\"}]").get("bank"));

        DocumentReadException e = assertThrows(DocumentReadException.class, () -> processor.parse("{\"dueDate\":\"mañana\"}"));
        assertEquals(ReadFailure.INVALID_RESPONSE, e.failure());
    }
}
