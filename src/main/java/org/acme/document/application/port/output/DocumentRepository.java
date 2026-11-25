package org.acme.document.application.port.output;

import org.acme.document.domain.model.Document;

import java.util.Optional;
import java.util.UUID;

/** Documentos de un usuario. Toda consulta va filtrada por userId y excluye los borrados. */
public interface DocumentRepository {

    /** Inserta si no tiene id; si no, actualiza. Devuelve el documento persistido. */
    Document save(Document document);

    /** Documento no borrado del usuario con ese SHA-256 (para detectar el mismo archivo). */
    Optional<Document> findActiveByChecksum(UUID userId, String checksum);
}
