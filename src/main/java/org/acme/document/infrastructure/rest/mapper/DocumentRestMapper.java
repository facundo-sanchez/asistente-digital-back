package org.acme.document.infrastructure.rest.mapper;

import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.acme.document.infrastructure.rest.dto.DocumentResponse;
import org.acme.document.infrastructure.rest.dto.DocumentUpdateRequest;
import org.acme.document.infrastructure.rest.dto.ExtractionResultResponse;
import org.acme.document.infrastructure.rest.dto.ProcessingJobResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface DocumentRestMapper {

    DocumentResponse toResponse(Document document);

    ProcessingJobResponse toResponse(DocumentProcessingJob job);

    ExtractionResultResponse toResponse(AiExtractionResult result);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "originalFilename", ignore = true)
    @Mapping(target = "storageKey", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "checksum", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "processingStatus", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(DocumentUpdateRequest request, @MappingTarget Document document);
}
