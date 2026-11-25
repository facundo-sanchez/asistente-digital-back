package org.acme.document.infrastructure.rest.mapper;

import org.acme.document.application.model.DocumentView;
import org.acme.document.application.model.UpdateDocumentCommand;
import org.acme.document.domain.model.AiExtractionResult;
import org.acme.document.domain.model.Document;
import org.acme.document.domain.model.DocumentProcessingJob;
import org.acme.document.infrastructure.rest.dto.DocumentResponse;
import org.acme.document.infrastructure.rest.dto.DocumentUpdateRequest;
import org.acme.document.infrastructure.rest.dto.ExtractionResultResponse;
import org.acme.document.infrastructure.rest.dto.ProcessingJobResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface DocumentRestMapper {

    /** serviceName llega aparte: el documento solo guarda el id del servicio. */
    DocumentResponse toResponse(Document document, String serviceName);

    default DocumentResponse toResponse(DocumentView view) {
        return toResponse(view.document(), view.serviceName());
    }

    ProcessingJobResponse toResponse(DocumentProcessingJob job);

    ExtractionResultResponse toResponse(AiExtractionResult result);

    UpdateDocumentCommand toCommand(DocumentUpdateRequest request);
}
