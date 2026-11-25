package org.acme.obligation.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Escritura de obligaciones. */
@Entity
@Table(name = "obligations")
public class ObligationEntity extends ObligationColumns {
}
