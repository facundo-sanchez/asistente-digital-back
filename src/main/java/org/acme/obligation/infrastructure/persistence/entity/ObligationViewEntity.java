package org.acme.obligation.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/** Lectura de obligaciones desde la vista v_obligations (saldo y estado efectivo). */
@Entity
@Immutable
@Table(name = "v_obligations")
@Getter
@Setter
public class ObligationViewEntity extends ObligationColumns {

    @Column(name = "paid_amount", precision = 19, scale = 4)
    private BigDecimal paidAmount;

    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "effective_status", length = 30)
    private EffectiveObligationStatus effectiveStatus;
}
