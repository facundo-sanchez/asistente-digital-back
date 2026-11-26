package org.acme.document.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.document.application.DocumentSettings;
import org.acme.document.application.FileType;
import org.acme.document.application.model.UploadDocumentCommand;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.application.port.output.FileStorage;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.DocumentSource;
import org.acme.document.domain.model.ProcessingStatus;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.service.application.port.output.ServiceRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** Subir un documento (CU-009): valida el archivo, lo guarda y registra sus datos. */
@ApplicationScoped
public class UploadDocumentUseCase {

    static final String EMPTY_FILE = "El archivo está vacío";
    static final String INVALID_TYPE = "Solo se admiten archivos PDF, JPG, PNG, WEBP o Excel";
    static final String DUPLICATE = "Ya subiste este archivo: «%s»";

    private final DocumentRepository documents;
    private final FileStorage storage;
    private final DocumentSettings settings;
    private final ServiceRepository services;

    @Inject
    public UploadDocumentUseCase(DocumentRepository documents, FileStorage storage, DocumentSettings settings, ServiceRepository services) {
        this.documents = documents;
        this.storage = storage;
        this.settings = settings;
        this.services = services;
    }

    @Transactional
    public Document upload(UUID userId, UploadDocumentCommand command) {
        try {
            long size = Files.size(command.file());
            if (size == 0) {
                throw new ServiceException(EMPTY_FILE);
            }
            if (size > settings.maxSize()) {
                throw new ServiceException("El archivo supera el máximo de " + settings.maxSize() / (1024 * 1024) + " MB");
            }
            // Un servicio de otro usuario responde igual que uno inexistente
            if (command.serviceId() != null && services.findByIdForUser(userId, command.serviceId()).isEmpty()) {
                throw new ServiceException(ManageDocumentUseCase.UNKNOWN_SERVICE);
            }
            String mimeType = FileType.detect(command.file()).orElseThrow(() -> new ServiceException(INVALID_TYPE));

            String checksum = sha256(command);
            documents.findActiveByChecksum(userId, checksum).ifPresent(existing -> {
                throw new ConflictException(DUPLICATE.formatted(displayName(existing)));
            });

            String storageKey = userId + "/" + UUID.randomUUID();
            storage.save(storageKey, command.file());
            try {
                return documents.save(Document.builder()
                        .userId(userId)
                        .serviceId(command.serviceId())
                        .documentType(command.documentType())
                        .title(blankToNull(command.title()))
                        .originalFilename(cleanFilename(command.originalFilename()))
                        .storageKey(storageKey)
                        .mimeType(mimeType)
                        .fileSize(size)
                        .checksum(checksum)
                        .source(DocumentSource.MANUAL)
                        // La extracción con IA llega en el módulo 9
                        .processingStatus(ProcessingStatus.NOT_REQUIRED)
                        .build());
            } catch (RuntimeException e) {
                // Sin fila en la base, el archivo guardado quedaría huérfano
                storage.delete(storageKey);
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
    }

    /** SHA-256 del archivo leyéndolo de a bloques (no se carga entero en memoria). */
    private static String sha256(UploadDocumentCommand command) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
        try (InputStream in = Files.newInputStream(command.file())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Solo el nombre (sin carpetas), sin caracteres de control y de hasta 255 caracteres. */
    static String cleanFilename(String filename) {
        String name = filename == null ? "" : filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            name = "documento";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static String displayName(Document document) {
        return document.getTitle() != null ? document.getTitle() : document.getOriginalFilename();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
