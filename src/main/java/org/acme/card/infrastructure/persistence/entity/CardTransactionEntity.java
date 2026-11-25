package org.acme.card.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.card.domain.model.CardTransactionType;
import org.acme.card.domain.model.CategorySource;
import org.acme.card.domain.model.SpendingCategory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "card_transactions")
@Getter
@Setter
public class CardTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "statement_id", nullable = false)
    private UUID statementId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "merchant_name", length = 150)
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private SpendingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_source", nullable = false, length = 20)
    private CategorySource categorySource;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private CardTransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "installment_number", columnDefinition = "smallint")
    private Integer installmentNumber;

    @Column(name = "installment_total", columnDefinition = "smallint")
    private Integer installmentTotal;

    @Column(name = "original_amount", precision = 19, scale = 4)
    private BigDecimal originalAmount;

    @Column(name = "purchase_group_id")
    private UUID purchaseGroupId;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
