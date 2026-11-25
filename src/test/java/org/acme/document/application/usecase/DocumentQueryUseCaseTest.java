package org.acme.document.application.usecase;

import org.acme.document.application.model.DocumentView;
import org.acme.document.domain.model.Document;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.domain.model.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Repositorio documental: aislamiento, borrados, filtros, orden y nombre del servicio. */
class DocumentQueryUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();
    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);

    private InMemoryDocumentRepository documents;
    private DocumentQueryUseCase useCase;
    private UUID light;
    private UUID invoice;

    @BeforeEach
    void setUp() {
        documents = new InMemoryDocumentRepository();
        InMemoryServiceRepository services = new InMemoryServiceRepository();
        light = services.save(Service.builder().userId(ANA).providerId(UUID.randomUUID()).name("Luz casa").build()).getId();
        useCase = new DocumentQueryUseCase(documents, services);

        invoice = add(ANA, "Factura luz octubre", "luz-10.pdf", DocumentType.INVOICE, light, 1);
        add(ANA, null, "poliza-auto.pdf", DocumentType.POLICY, null, 2);
        add(BETO, "Factura de Beto", "beto.pdf", DocumentType.INVOICE, null, 3);
        Document deleted = documents.rows.get(documents.rows.size() - 1).toBuilder().id(null).userId(ANA).title("Borrada").build();
        deleted.delete(Instant.now());
        documents.save(deleted);
    }

    @Test
    void listaSoloLosDocumentosDelUsuarioSinBorradosYMasNuevosPrimero() {
        assertEquals(List.of("poliza-auto.pdf", "luz-10.pdf"), filenames(useCase.list(ANA, null, null, null, FIRST_PAGE)));
    }

    @Test
    void buscaEnTituloYNombreDeArchivoYFiltraPorTipoYServicio() {
        assertEquals(List.of("luz-10.pdf"), filenames(useCase.list(ANA, " OCTUBRE ", null, null, FIRST_PAGE)));
        assertEquals(List.of("poliza-auto.pdf"), filenames(useCase.list(ANA, "auto", null, null, FIRST_PAGE)));
        assertEquals(List.of("poliza-auto.pdf"), filenames(useCase.list(ANA, null, DocumentType.POLICY, null, FIRST_PAGE)));
        assertEquals(List.of("luz-10.pdf"), filenames(useCase.list(ANA, null, null, light, FIRST_PAGE)));
    }

    @Test
    void incluyeElNombreDelServicioAsociado() {
        DocumentView view = useCase.get(ANA, invoice);

        assertEquals("Luz casa", view.serviceName());
        assertNull(useCase.list(ANA, "poliza", null, null, FIRST_PAGE).content().getFirst().serviceName());
    }

    @Test
    void unDocumentoDeOtroUsuarioRespondeComoInexistente() {
        assertThrows(NotFoundException.class, () -> useCase.get(BETO, invoice));
        assertThrows(NotFoundException.class, () -> useCase.get(ANA, UUID.randomUUID()));
    }

    private UUID add(UUID userId, String title, String filename, DocumentType type, UUID serviceId, int minutesAgo) {
        return documents.save(Document.builder()
                .userId(userId).title(title).originalFilename(filename).documentType(type).serviceId(serviceId)
                .checksum(UUID.randomUUID().toString()).storageKey("k").mimeType("application/pdf").fileSize(10)
                .createdAt(Instant.now().minusSeconds(60L * (10 - minutesAgo)))
                .build()).getId();
    }

    private static List<String> filenames(PageResult<DocumentView> page) {
        return page.content().stream().map(v -> v.document().getOriginalFilename()).toList();
    }
}
