package org.acme.payment.infrastructure.rest.mapper;

import org.acme.payment.domain.model.Payment;
import org.acme.payment.infrastructure.rest.dto.PaymentRequest;
import org.acme.payment.infrastructure.rest.dto.PaymentResponse;
import org.acme.shared.infrastructure.mapper.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface PaymentRestMapper {

    PaymentResponse toResponse(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "voidedAt", ignore = true)
    @Mapping(target = "voidReason", ignore = true)
    @Mapping(target = "replacesPaymentId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Payment toModel(PaymentRequest request);
}
