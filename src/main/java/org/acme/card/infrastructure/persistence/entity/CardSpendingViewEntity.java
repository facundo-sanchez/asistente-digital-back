package org.acme.card.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.card.domain.model.CardTransactionType;
import org.acme.card.domain.model.SpendingCategory;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista v_card_spending: consumos de resúmenes confirmados, sin pagos del resumen.
 * Se usa en las consultas agregadas del informe (GROUP BY categoría, mes, tarjeta, comercio).
 */
@Entity
@Immutable
@Table(name = "v_card_spending")
@Getter
@Setter
public class CardSpendingViewEntity {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "statement_id")
    private UUID statementId;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Column(name = "billing_month")
    private LocalDate billingMonth;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "merchant")
    private String merchant;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private SpendingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private CardTransactionType transactionType;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", columnDefinition = "char(3)")
    private String currency;
}
