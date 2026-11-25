package org.acme.invoice.infrastructure.rest.mapper;

import org.acme.invoice.domain.model.Invoice;
import org.acme.invoice.infrastructure.rest.dto.InvoiceRequest;
import org.acme.invoice.infrastructure.rest.dto.InvoiceResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface InvoiceRestMapper {

    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "extractionResultId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "confidenceScore", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Invoice toModel(InvoiceRequest request);

    @InheritConfiguration(name = "toModel")
    void update(InvoiceRequest request, @MappingTarget Invoice invoice);
}
