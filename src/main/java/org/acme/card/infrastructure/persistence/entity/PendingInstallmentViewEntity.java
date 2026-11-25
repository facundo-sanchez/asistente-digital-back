package org.acme.card.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.card.domain.model.SpendingCategory;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Vista v_card_pending_installments: compras en cuotas con cuotas restantes. */
@Entity
@Immutable
@Table(name = "v_card_pending_installments")
@Getter
@Setter
public class PendingInstallmentViewEntity {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "purchase_group_id")
    private UUID purchaseGroupId;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "merchant")
    private String merchant;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private SpendingCategory category;

    @Column(name = "currency", columnDefinition = "char(3)")
    private String currency;

    @Column(name = "installment_amount", precision = 19, scale = 4)
    private BigDecimal installmentAmount;

    @Column(name = "installment_number", columnDefinition = "smallint")
    private Integer installmentNumber;

    @Column(name = "installment_total", columnDefinition = "smallint")
    private Integer installmentTotal;

    @Column(name = "remaining_installments", columnDefinition = "smallint")
    private Integer remainingInstallments;

    @Column(name = "remaining_amount", precision = 19, scale = 4)
    private BigDecimal remainingAmount;

    @Column(name = "original_amount", precision = 19, scale = 4)
    private BigDecimal originalAmount;

    @Column(name = "last_closing_date")
    private LocalDate lastClosingDate;

    @Column(name = "estimated_last_closing_date")
    private LocalDate estimatedLastClosingDate;
}
