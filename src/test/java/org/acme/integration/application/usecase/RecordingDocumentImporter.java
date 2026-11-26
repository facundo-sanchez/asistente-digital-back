package org.acme.integration.application.usecase;

import org.acme.domain.exceptions.ConflictException;
import org.acme.integration.application.port.output.DocumentImporter;
import org.acme.shared.domain.model.DocumentType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Anota lo importado; el mismo contenido dos veces es duplicado, como en Documentos. */
public class RecordingDocumentImporter implements DocumentImporter {

    public final List<String> imported = new ArrayList<>();
    public final List<UUID> services = new ArrayList<>();
    private final Set<String> contents = new HashSet<>();
    /** La próxima importación falla (ej. la base no respondió). */
    public boolean failNext;

    @Override
    public UUID importFile(UUID userId, Path file, String filename, DocumentType type, UUID serviceId) {
        try {
            return add(Files.readString(file), filename + " (" + type + ")", serviceId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public UUID importText(UUID userId, String text, String filename, DocumentType type, UUID serviceId) {
        return add(text, filename + " (" + type + ")", serviceId);
    }

    private UUID add(String content, String description, UUID serviceId) {
        if (failNext) {
            failNext = false;
            throw new IllegalStateException("falla de prueba");
        }
        if (!contents.add(content)) throw new ConflictException("duplicado");
        imported.add(description);
        services.add(serviceId);
        return UUID.randomUUID();
    }
}
