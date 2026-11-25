package org.acme.payment.infrastructure.persistence.repository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.payment.application.port.output.PaymentRepository;
import org.acme.payment.domain.model.Payment;
import org.acme.payment.infrastructure.persistence.entity.PaymentEntity;
import org.acme.payment.infrastructure.persistence.mapper.PaymentPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<Payment> findByIdForUser(UUID userId, UUID paymentId) {
        return repository.find("id = ?1 and userId = ?2", paymentId, userId).firstResultOptional().map(mapper::toModel);
    }

    @Override
    public List<Payment> findForObligation(UUID userId, UUID obligationId) {
        return repository.list("userId = ?1 and obligationId = ?2", Sort.descending("paymentDate").and("createdAt", Sort.Direction.Descending),
                userId, obligationId).stream().map(mapper::toModel).toList();
    }
}
