package org.acme.card.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.card.domain.model.CardBrand;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@Setter
public class CardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "brand", nullable = false, length = 30)
    private CardBrand brand;

    @Column(name = "last_four", columnDefinition = "char(4)")
    private String lastFour;

    @Column(name = "account_last_four", columnDefinition = "char(4)")
    private String accountLastFour;

    @Column(name = "holder_name", length = 150)
    private String holderName;

    @Column(name = "additional", nullable = false)
    private boolean additional;

    @Column(name = "closing_day", columnDefinition = "smallint")
    private Integer closingDay;

    @Column(name = "due_day", columnDefinition = "smallint")
    private Integer dueDay;

    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit;

    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
