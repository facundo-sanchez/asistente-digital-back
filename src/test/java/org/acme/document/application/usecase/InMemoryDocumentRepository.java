package org.acme.document.application.usecase;

import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.domain.model.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositorio en memoria. failOnSave simula un error de la base. */
public class InMemoryDocumentRepository implements DocumentRepository {

    final List<Document> rows = new ArrayList<>();
    boolean failOnSave;

    @Override
    public Document save(Document document) {
        if (failOnSave) {
            throw new IllegalStateException("La base no responde");
        }
        Document copy = document.toBuilder().build();
        if (copy.getId() == null) {
            copy.setId(UUID.randomUUID());
        }
        rows.removeIf(d -> d.getId().equals(copy.getId()));
        rows.add(copy);
        return copy.toBuilder().build();
    }

    @Override
    public Optional<Document> findActiveByChecksum(UUID userId, String checksum) {
        return rows.stream()
                .filter(d -> d.getUserId().equals(userId) && d.getChecksum().equals(checksum) && !d.isDeleted())
                .findFirst();
    }
}
