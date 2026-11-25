package org.acme.document.application.port.output;

import org.acme.document.application.model.DocumentFilter;
import org.acme.document.domain.model.Document;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.Optional;
import java.util.UUID;

/** Documentos de un usuario. Toda consulta va filtrada por userId y excluye los borrados. */
public interface DocumentRepository {

    /** Inserta si no tiene id; si no, actualiza. Devuelve el documento persistido. */
    Document save(Document document);

    /** No borrado y del usuario. */
    Optional<Document> findByIdForUser(UUID userId, UUID documentId);

    /** No borrados, más nuevos primero. */
    PageResult<Document> findForUser(UUID userId, DocumentFilter filter, PageQuery page);

    /** Documento no borrado del usuario con ese SHA-256 (para detectar el mismo archivo). */
    Optional<Document> findActiveByChecksum(UUID userId, String checksum);
}
