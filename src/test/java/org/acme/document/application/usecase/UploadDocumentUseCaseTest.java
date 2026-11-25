package org.acme.document.application.usecase;

import org.acme.document.application.model.UploadDocumentCommand;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.ProcessingStatus;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.shared.domain.model.DocumentType;
import org.acme.service.application.usecase.InMemoryServiceRepository;
import org.acme.service.domain.model.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Subida de documentos: tipo real, tamaño, repetido, dueño y limpieza ante errores. */
class UploadDocumentUseCaseTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BETO = UUID.randomUUID();

    @TempDir
    Path tmp;

    private InMemoryDocumentRepository documents;
    private InMemoryFileStorage storage;
    private UploadDocumentUseCase useCase;
    private UUID anaLight;
    private UUID betoGas;

    @BeforeEach
    void setUp() {
        documents = new InMemoryDocumentRepository();
        storage = new InMemoryFileStorage();
        InMemoryServiceRepository services = new InMemoryServiceRepository();
        anaLight = services.save(Service.builder().userId(ANA).providerId(UUID.randomUUID()).name("Luz casa").build()).getId();
        betoGas = services.save(Service.builder().userId(BETO).providerId(UUID.randomUUID()).name("Gas Beto").build()).getId();
        useCase = new UploadDocumentUseCase(documents, storage, new TestDocumentSettings(), services);
    }

    @Test
    void guardaUnPdfConSusDatos() throws IOException {
        Document saved = useCase.upload(ANA, command(pdf("factura"), "C:\\Descargas\\factura-luz.pdf", " Luz octubre "));

        assertEquals(ANA, saved.getUserId());
        assertEquals("application/pdf", saved.getMimeType());
        assertEquals("factura-luz.pdf", saved.getOriginalFilename(), "sin las carpetas del usuario");
        assertEquals("Luz octubre", saved.getTitle());
        assertEquals(ProcessingStatus.NOT_REQUIRED, saved.getProcessingStatus());
        assertEquals(64, saved.getChecksum().length());
        assertTrue(saved.getStorageKey().startsWith(ANA + "/"));
        assertTrue(storage.files.containsKey(saved.getStorageKey()));
    }

    @Test
    void quedaAsociadoAlServicioDelUsuarioYRechazaUnoAjeno() throws IOException {
        Document saved = useCase.upload(ANA, new UploadDocumentCommand(pdf("luz"), "luz.pdf", DocumentType.INVOICE, null, anaLight));
        assertEquals(anaLight, saved.getServiceId());

        var foreign = new UploadDocumentCommand(pdf("gas"), "gas.pdf", DocumentType.INVOICE, null, betoGas);
        assertThrows(ServiceException.class, () -> useCase.upload(ANA, foreign));
        assertEquals(1, storage.files.size(), "no guarda el archivo si el servicio no es válido");
    }

    @Test
    void rechazaUnArchivoQueNoEsDeUnTipoAdmitidoAunqueSeLlamePdf() throws IOException {
        Path exe = file("MZ ejecutable disfrazado".getBytes(StandardCharsets.UTF_8));

        ServiceException e = assertThrows(ServiceException.class, () -> useCase.upload(ANA, command(exe, "factura.pdf", null)));

        assertEquals(UploadDocumentUseCase.INVALID_TYPE, e.getMessage());
        assertTrue(storage.files.isEmpty());
    }

    @Test
    void rechazaArchivosVaciosYDemasiadoGrandes() throws IOException {
        assertThrows(ServiceException.class, () -> useCase.upload(ANA, command(file(new byte[0]), "vacio.pdf", null)));

        byte[] big = new byte[(int) TestDocumentSettings.MAX_SIZE + 1];
        System.arraycopy("%PDF".getBytes(StandardCharsets.US_ASCII), 0, big, 0, 4);
        assertThrows(ServiceException.class, () -> useCase.upload(ANA, command(file(big), "grande.pdf", null)));
    }

    @Test
    void elMismoArchivoDelMismoUsuarioEsRepetidoPeroOtroUsuarioPuedeSubirlo() throws IOException {
        Path pdf = pdf("igual");
        useCase.upload(ANA, command(pdf, "a.pdf", "Primera"));

        ConflictException e = assertThrows(ConflictException.class, () -> useCase.upload(ANA, command(pdf, "b.pdf", null)));
        assertTrue(e.getMessage().contains("Primera"));

        assertDoesNotThrow(() -> useCase.upload(BETO, command(pdf, "a.pdf", null)));
    }

    @Test
    void siFallaLaBaseBorraElArchivoGuardado() throws IOException {
        documents.failOnSave = true;

        assertThrows(IllegalStateException.class, () -> useCase.upload(ANA, command(pdf("x"), "x.pdf", null)));

        assertTrue(storage.files.isEmpty(), "no queda un archivo huérfano");
    }

    @Test
    void limpiaElNombreDelArchivo() {
        assertEquals("a.pdf", UploadDocumentUseCase.cleanFilename("../../etc/a.pdf"));
        assertEquals("documento", UploadDocumentUseCase.cleanFilename("  "));
    }

    private Path pdf(String content) throws IOException {
        return file(("%PDF-1.7 " + content).getBytes(StandardCharsets.US_ASCII));
    }

    private Path file(byte[] content) throws IOException {
        return Files.write(Files.createTempFile(tmp, "subida", ".tmp"), content);
    }

    private static UploadDocumentCommand command(Path file, String filename, String title) {
        return new UploadDocumentCommand(file, filename, DocumentType.INVOICE, title, null);
    }
}
