package org.acme.document.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.domain.model.Document;
import org.acme.document.infrastructure.persistence.entity.DocumentEntity;
import org.acme.document.infrastructure.persistence.mapper.DocumentPersistenceMapper;

import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link DocumentRepository}. */
@ApplicationScoped
public class DocumentRepositoryAdapter implements DocumentRepository {

    @Inject
    DocumentPanacheRepository repository;

    @Inject
    DocumentPersistenceMapper mapper;

    @Override
    public Document save(Document document) {
        DocumentEntity entity = mapper.toEntity(document);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<Document> findActiveByChecksum(UUID userId, String checksum) {
        return repository.find("userId = ?1 and checksum = ?2 and deletedAt is null", userId, checksum)
                .firstResultOptional()
                .map(mapper::toModel);
    }
}
