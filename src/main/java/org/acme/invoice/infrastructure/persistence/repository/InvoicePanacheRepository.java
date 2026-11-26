package org.acme.invoice.infrastructure.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.invoice.infrastructure.persistence.entity.InvoiceEntity;

import java.util.UUID;

@ApplicationScoped
public class InvoicePanacheRepository implements PanacheRepositoryBase<InvoiceEntity, UUID> {
}
