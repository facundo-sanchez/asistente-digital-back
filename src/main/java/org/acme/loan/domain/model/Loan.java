package org.acme.loan.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Un préstamo que tomó el usuario (módulo 13). Sus cuotas son vencimientos de tipo LOAN. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    private UUID id;
    private UUID userId;
    /** Banco, financiera o persona. */
    private String lender;
    private String name;
    /** Número del préstamo en el comprobante del banco (identifica sus comprobantes de pago). */
    private String loanNumber;
    /** Referencia corta que usa el banco en los avisos por email (ej. "203/2"). */
    private String shortReference;
    private String currency;
    private BigDecimal principal;
    private LoanRateType rateType;
    /** TNA en %. */
    private BigDecimal annualRate;
    private LocalDate disbursementDate;
    private int installmentsCount;
    private LocalDate firstDueDate;
    private UUID contractDocumentId;
    /** Último saldo de capital informado por el banco y su fecha. */
    private BigDecimal bankBalance;
    private LocalDate bankBalanceDate;
    private String notes;
    @Builder.Default
    private LoanStatus status = LoanStatus.ACTIVE;
    private String cancelReason;
    private Instant createdAt;
    private Instant updatedAt;
}
