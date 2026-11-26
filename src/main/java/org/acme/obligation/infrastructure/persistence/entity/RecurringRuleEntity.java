package org.acme.obligation.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.obligation.domain.model.ObligationType;
import org.acme.obligation.domain.model.RecurrenceOrigin;
import org.acme.shared.domain.model.BillingFrequency;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recurring_rules")
@Getter
@Setter
public class RecurringRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_type", nullable = false, length = 50)
    private ObligationType obligationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 30)
    private BillingFrequency frequency;

    // SMALLINT en la base: sin esto Hibernate valida la columna como INTEGER

    @JdbcTypeCode(SqlTypes.SMALLINT)

    @Column(name = "day_of_month", columnDefinition = "smallint")
    private Integer dayOfMonth;

    @Column(name = "next_expected_date", nullable = false)
    private LocalDate nextExpectedDate;

    @Column(name = "estimated_amount", precision = 19, scale = 4)
    private BigDecimal estimatedAmount;

    // CHAR(3) en la base: sin esto Hibernate valida la columna como VARCHAR
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", columnDefinition = "char(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20)
    private RecurrenceOrigin origin;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
