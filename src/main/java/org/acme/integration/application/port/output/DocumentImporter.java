package org.acme.integration.application.port.output;

import org.acme.shared.domain.model.DocumentType;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Guarda en Documentos lo que llegó por email (origen Gmail) y lo manda a leer con IA.
 * Si el mismo archivo ya está, lanza ConflictException.
 */
public interface DocumentImporter {

    UUID importFile(UUID userId, Path file, String filename, DocumentType type, UUID serviceId);

    /** Para los emails que traen la factura en el cuerpo, sin adjunto. */
    UUID importText(UUID userId, String text, String filename, DocumentType type, UUID serviceId);
}
