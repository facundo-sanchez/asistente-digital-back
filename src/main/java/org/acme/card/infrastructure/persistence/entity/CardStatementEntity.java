package org.acme.card.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.card.domain.model.CardStatementSource;
import org.acme.card.domain.model.CardStatementStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "card_statements")
@Getter
@Setter
public class CardStatementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "extraction_result_id")
    private UUID extractionResultId;

    @Column(name = "obligation_id")
    private UUID obligationId;

    @Column(name = "obligation_usd_id")
    private UUID obligationUsdId;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "next_closing_date")
    private LocalDate nextClosingDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "total_amount_usd", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmountUsd;

    @Column(name = "minimum_payment", precision = 19, scale = 4)
    private BigDecimal minimumPayment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CardStatementStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private CardStatementSource source;

    @Column(name = "source_message_id")
    private String sourceMessageId;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
