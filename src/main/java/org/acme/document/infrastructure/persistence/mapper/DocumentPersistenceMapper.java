package org.acme.document.infrastructure.persistence.mapper;

import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.acme.document.infrastructure.persistence.entity.AiExtractionResultEntity;
import org.acme.document.infrastructure.persistence.entity.DocumentEntity;
import org.acme.document.infrastructure.persistence.entity.DocumentProcessingJobEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface DocumentPersistenceMapper {
    Document toModel(DocumentEntity entity);
    DocumentEntity toEntity(Document model);

    DocumentProcessingJob toModel(DocumentProcessingJobEntity entity);
    DocumentProcessingJobEntity toEntity(DocumentProcessingJob model);

    AiExtractionResult toModel(AiExtractionResultEntity entity);
    AiExtractionResultEntity toEntity(AiExtractionResult model);
}
