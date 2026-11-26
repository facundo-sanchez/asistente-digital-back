package org.acme.integration.infrastructure.document;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.model.UploadDocumentCommand;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.application.port.output.FileStorage;
import org.acme.document.application.usecase.DocumentProcessingUseCase;
import org.acme.document.application.usecase.UploadDocumentUseCase;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.DocumentSource;
import org.acme.document.domain.model.ProcessingStatus;
import org.acme.document.infrastructure.ai.AiInputs;
import org.acme.domain.exceptions.ConflictException;
import org.acme.integration.application.port.output.DocumentImporter;
import org.acme.shared.domain.model.DocumentType;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** Lo que llega por Gmail entra a Documentos (origen GMAIL) y a la cola de la IA. */
@ApplicationScoped
public class DocumentImporterAdapter implements DocumentImporter {

    private static final Logger LOG = Logger.getLogger(DocumentImporterAdapter.class);

    @Inject
    UploadDocumentUseCase upload;

    @Inject
    DocumentProcessingUseCase processing;

    @Inject
    DocumentRepository documents;

    @Inject
    FileStorage storage;

    @Override
    public UUID importFile(UUID userId, Path file, String filename, DocumentType type, UUID serviceId) {
        Document document = upload.upload(userId, new UploadDocumentCommand(file, filename, type, null, serviceId, DocumentSource.GMAIL));
        return enqueue(userId, document);
    }

    /** Las validaciones de la subida (tipos de archivo) no aplican: es texto que armamos nosotros. */
    @Override
    public UUID importText(UUID userId, String text, String filename, DocumentType type, UUID serviceId) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        String checksum = sha256(bytes);
        if (documents.findActiveByChecksum(userId, checksum).isPresent()) {
            throw new ConflictException("Ese email ya está importado");
        }
        String storageKey = userId + "/" + UUID.randomUUID();
        Path temp = null;
        try {
            temp = Files.createTempFile("gmail-", ".txt");
            Files.write(temp, bytes);
            storage.save(storageKey, temp);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteQuietly(temp);
        }
        Document document = documents.save(Document.builder()
                .userId(userId)
                .serviceId(serviceId)
                .documentType(type)
                .originalFilename(filename)
                .storageKey(storageKey)
                .mimeType(AiInputs.TEXT)
                .fileSize(bytes.length)
                .checksum(checksum)
                .source(DocumentSource.GMAIL)
                .processingStatus(ProcessingStatus.NOT_REQUIRED)
                .build());
        return enqueue(userId, document);
    }

    /** Si la IA no está disponible el documento igual queda importado; se puede leer después desde su detalle. */
    private UUID enqueue(UUID userId, Document document) {
        try {
            processing.request(userId, document.getId());
        } catch (RuntimeException e) {
            LOG.warnf("El documento %s quedó importado sin leer: %s", document.getId(), e.getClass().getSimpleName());
        }
        return document.getId();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Es un temporal
        }
    }
}
