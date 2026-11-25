package org.acme.document.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.model.DocumentFilter;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.document.domain.model.Document;
import org.acme.document.infrastructure.persistence.entity.DocumentEntity;
import org.acme.document.infrastructure.persistence.mapper.DocumentPersistenceMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.application.model.PageResult;
import org.acme.shared.infrastructure.persistence.LikePattern;

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
    public Optional<Document> findByIdForUser(UUID userId, UUID documentId) {
        return repository.find("id = ?1 and userId = ?2 and deletedAt is null", documentId, userId)
                .firstResultOptional()
                .map(mapper::toModel);
    }

    @Override
    public PageResult<Document> findForUser(UUID userId, DocumentFilter filter, PageQuery page) {
        StringBuilder where = new StringBuilder("userId = :userId and deletedAt is null");
        Parameters params = Parameters.with("userId", userId);
        if (filter.text() != null) {
            where.append(" and (lower(title) like :text").append(LikePattern.ESCAPE)
                    .append(" or lower(originalFilename) like :text").append(LikePattern.ESCAPE).append(")");
            params.and("text", LikePattern.contains(filter.text()));
        }
        if (filter.documentType() != null) {
            where.append(" and documentType = :type");
            params.and("type", filter.documentType());
        }
        if (filter.serviceId() != null) {
            where.append(" and serviceId = :serviceId");
            params.and("serviceId", filter.serviceId());
        }

        PanacheQuery<DocumentEntity> query = repository.find(where.toString(), Sort.descending("createdAt"), params)
                .page(Page.of(page.page(), page.size()));
        return new PageResult<>(query.list().stream().map(mapper::toModel).toList(), page.page(), page.size(), query.count());
    }

    @Override
    public Optional<Document> findActiveByChecksum(UUID userId, String checksum) {
        return repository.find("userId = ?1 and checksum = ?2 and deletedAt is null", userId, checksum)
                .firstResultOptional()
                .map(mapper::toModel);
    }
}
