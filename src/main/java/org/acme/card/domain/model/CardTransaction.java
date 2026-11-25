package org.acme.card.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Movimiento de un resumen de tarjeta (CU-016).
 * amount tiene signo, como figura en el resumen: positivo = cargo, negativo = crédito.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CardTransaction {
    private UUID id;
    private UUID userId;
    private UUID cardId;
    private UUID statementId;
    /** En compras en cuotas, la fecha original de compra. */
    private LocalDate transactionDate;
    /** Texto tal como figura en el resumen. */
    private String description;
    /** Comercio normalizado (mayúsculas, sin sucursal). */
    private String merchantName;
    @Builder.Default
    private SpendingCategory category = SpendingCategory.OTHER;
    @Builder.Default
    private CategorySource categorySource = CategorySource.AI;
    @Builder.Default
    private CardTransactionType transactionType = CardTransactionType.PURCHASE;
    private BigDecimal amount;
    private String currency;
    private Integer installmentNumber;
    private Integer installmentTotal;
    /** Importe total de la compra en cuotas. */
    private BigDecimal originalAmount;
    /** Agrupa las cuotas de una misma compra entre resúmenes (RN-020). */
    private UUID purchaseGroupId;
    /** Nro. de cupón / comprobante. */
    private String reference;
    private BigDecimal confidenceScore;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isCredit() {
        return amount != null && amount.signum() < 0;
    }

    public boolean isInstallment() {
        return installmentTotal != null && installmentTotal > 1;
    }

    /** Recategorización. Una categoría puesta por el usuario no la pisa una regla ni la IA. */
    public void recategorize(SpendingCategory newCategory, CategorySource by) {
        if (categorySource == CategorySource.USER && by != CategorySource.USER) {
            return;
        }
        category = newCategory;
        categorySource = by;
    }
}
