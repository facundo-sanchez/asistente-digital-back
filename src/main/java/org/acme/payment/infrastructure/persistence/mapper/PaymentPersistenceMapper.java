package org.acme.payment.infrastructure.persistence.mapper;

import org.acme.payment.domain.model.Payment;
import org.acme.payment.infrastructure.persistence.entity.PaymentEntity;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface PaymentPersistenceMapper {
    Payment toModel(PaymentEntity entity);
    PaymentEntity toEntity(Payment model);
}
