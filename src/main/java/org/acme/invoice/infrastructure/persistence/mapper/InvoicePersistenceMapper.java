package org.acme.invoice.infrastructure.persistence.mapper;

import org.acme.invoice.domain.model.Invoice;
import org.acme.invoice.infrastructure.persistence.entity.InvoiceEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface InvoicePersistenceMapper {
    Invoice toModel(InvoiceEntity entity);
    InvoiceEntity toEntity(Invoice model);
}
