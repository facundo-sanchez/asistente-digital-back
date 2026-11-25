package org.acme.document.application.usecase;

import org.acme.document.application.model.DocumentView;
import org.acme.document.application.model.UpdateDocumentCommand;
import org.acme.document.domain.model.Document;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Edición, asociación a un servicio y baja lógica de documentos. */
class ManageDocumentUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();
    private static final LocalDate DUE = LocalDate.of(2026, 12, 15);

    private InMemoryDocumentRepository documents;
    private ManageDocumentUseCase useCase;
    private UUID anaLight;
    private UUID betoGas;
    private UUID invoice;

    @BeforeEach
    void setUp() {
        documents = new InMemoryDocumentRepository();
        InMemoryServiceRepository services = new InMemoryServiceRepository();
        anaLight = services.save(Service.builder().userId(ANA).providerId(UUID.randomUUID()).name("Luz casa").build()).getId();
        betoGas = services.save(Service.builder().userId(BETO).providerId(UUID.randomUUID()).name("Gas Beto").build()).getId();
        useCase = new ManageDocumentUseCase(documents, services);
        invoice = documents.save(Document.builder().userId(ANA).originalFilename("luz.pdf").checksum("c").storageKey("k").build()).getId();
    }

    @Test
    void asociaUnServicioConVencimientoYLimpiaElTitulo() {
        DocumentView view = useCase.update(ANA, invoice, new UpdateDocumentCommand("  Factura dic  ", DocumentType.INVOICE, anaLight, DUE));

        assertEquals("Luz casa", view.serviceName());
        Document stored = documents.findByIdForUser(ANA, invoice).orElseThrow();
        assertEquals("Factura dic", stored.getTitle());
        assertEquals(DocumentType.INVOICE, stored.getDocumentType());
        assertEquals(anaLight, stored.getServiceId());
        assertEquals(DUE, stored.getExpiresOn());
    }

    @Test
    void quitaElServicioYElTituloVacio() {
        useCase.update(ANA, invoice, new UpdateDocumentCommand("Factura", DocumentType.INVOICE, anaLight, DUE));

        DocumentView view = useCase.update(ANA, invoice, new UpdateDocumentCommand(" ", DocumentType.OTHER, null, null));

        assertNull(view.serviceName());
        assertNull(view.document().getTitle());
        assertNull(view.document().getServiceId());
        assertNull(view.document().getExpiresOn());
    }

    @Test
    void noSePuedeAsociarUnServicioDeOtroUsuario() {
        var command = new UpdateDocumentCommand(null, DocumentType.INVOICE, betoGas, null);

        assertThrows(ServiceException.class, () -> useCase.update(ANA, invoice, command));
        assertNull(documents.findByIdForUser(ANA, invoice).orElseThrow().getServiceId());
    }

    @Test
    void laBajaEsLogicaYSoloParaElDuenio() {
        assertThrows(NotFoundException.class, () -> useCase.delete(BETO, invoice));

        useCase.delete(ANA, invoice);

        assertTrue(documents.findByIdForUser(ANA, invoice).isEmpty());
        assertTrue(documents.rows.getFirst().isDeleted());
        assertThrows(NotFoundException.class, () -> useCase.delete(ANA, invoice));
    }
}
