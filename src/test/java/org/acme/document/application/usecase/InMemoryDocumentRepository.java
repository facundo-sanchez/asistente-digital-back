package org.acme.document.application.usecase;

import org.acme.document.application.model.DocumentFilter;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.domain.model.Document;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Repositorio en memoria con los mismos filtros que el adaptador real. failOnSave simula un error de la base. */
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
    public Optional<Document> findByIdForUser(UUID userId, UUID documentId) {
        return rows.stream().filter(d -> d.getId().equals(documentId) && d.getUserId().equals(userId) && !d.isDeleted()).findFirst();
    }

    @Override
    public PageResult<Document> findForUser(UUID userId, DocumentFilter filter, PageQuery page) {
        List<Document> matches = rows.stream()
                .filter(d -> d.getUserId().equals(userId) && !d.isDeleted())
                .filter(d -> filter.text() == null || contains(d.getTitle(), filter.text()) || contains(d.getOriginalFilename(), filter.text()))
                .filter(d -> filter.documentType() == null || d.getDocumentType() == filter.documentType())
                .filter(d -> filter.serviceId() == null || filter.serviceId().equals(d.getServiceId()))
                .sorted(Comparator.comparing(Document::getCreatedAt).reversed())
                .toList();
        int from = Math.min(page.page() * page.size(), matches.size());
        int to = Math.min(from + page.size(), matches.size());
        return new PageResult<>(matches.subList(from, to), page.page(), page.size(), matches.size());
    }

    @Override
    public Optional<Document> findActiveByChecksum(UUID userId, String checksum) {
        return rows.stream()
                .filter(d -> d.getUserId().equals(userId) && d.getChecksum().equals(checksum) && !d.isDeleted())
                .findFirst();
    }

    private static boolean contains(String value, String text) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<Document> findExpiringBetween(UUID userId, LocalDate from, LocalDate to) {
        return rows.stream().filter(d -> d.getUserId().equals(userId) && !d.isDeleted() && d.getExpiresOn() != null
                        && !d.getExpiresOn().isBefore(from) && !d.getExpiresOn().isAfter(to))
                .sorted(Comparator.comparing(Document::getExpiresOn)).toList();
    }
}
