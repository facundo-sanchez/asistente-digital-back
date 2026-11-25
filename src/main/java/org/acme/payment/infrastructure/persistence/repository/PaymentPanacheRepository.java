package org.acme.payment.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.payment.infrastructure.persistence.entity.PaymentEntity;

import java.util.UUID;

@ApplicationScoped
public class PaymentPanacheRepository implements PanacheRepositoryBase<PaymentEntity, UUID> {
}
