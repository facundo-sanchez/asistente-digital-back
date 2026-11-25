package org.acme.payment.infrastructure.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.payment.application.port.output.PaymentRepository;
import org.acme.payment.domain.model.Payment;
import org.acme.payment.infrastructure.persistence.entity.PaymentEntity;
import org.acme.payment.infrastructure.persistence.mapper.PaymentPersistenceMapper;

/** Adaptador de persistencia del puerto {@link PaymentRepository}. */
@ApplicationScoped
public class PaymentRepositoryAdapter implements PaymentRepository {

    @Inject
    PaymentPanacheRepository repository;

    @Inject
    PaymentPersistenceMapper mapper;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = mapper.toEntity(payment);
        if (entity.getId() == null) {
            repository.persistAndFlush(entity);
        } else {
            entity = repository.getEntityManager().merge(entity);
            repository.flush();
        }
        return mapper.toModel(entity);
    }
}
