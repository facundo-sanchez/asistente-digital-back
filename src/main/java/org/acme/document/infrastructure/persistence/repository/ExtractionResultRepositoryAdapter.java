package org.acme.document.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.port.output.ExtractionResultRepository;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.ExtractionValidationStatus;
import org.acme.document.infrastructure.persistence.entity.AiExtractionResultEntity;
import org.acme.document.infrastructure.persistence.mapper.DocumentPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia del puerto {@link ExtractionResultRepository}. */
@ApplicationScoped
public class ExtractionResultRepositoryAdapter implements ExtractionResultRepository {

    @Inject
    ExtractionResultPanacheRepository repository;

    @Inject
    DocumentPersistenceMapper mapper;

    @Override
    public AiExtractionResult save(AiExtractionResult result) {
        AiExtractionResultEntity entity = mapper.toEntity(result);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }

    @Override
    public Optional<AiExtractionResult> findByIdForUser(UUID userId, UUID resultId) {
        return repository.find("id = ?1 and userId = ?2", resultId, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public Optional<AiExtractionResult> findLatestForDocument(UUID userId, UUID documentId) {
        return repository.find("userId = ?1 and documentId = ?2", Sort.descending("createdAt"), userId, documentId)
                .firstResultOptional()
                .map(mapper::toModel);
    }

    @Override
    public List<AiExtractionResult> findPending(UUID userId) {
        return repository.list("userId = ?1 and validationStatus = ?2", Sort.by("createdAt"), userId, ExtractionValidationStatus.PENDING)
                .stream().map(mapper::toModel).toList();
    }
}
