package org.acme.loan.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.loan.domain.model.LoanRateType;
import org.acme.loan.domain.model.LoanStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lender", nullable = false, length = 150)
    private String lender;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "loan_number", length = 60)
    private String loanNumber;

    @Column(name = "short_reference", length = 60)
    private String shortReference;

    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "principal", precision = 19, scale = 4)
    private BigDecimal principal;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", length = 20)
    private LoanRateType rateType;

    @Column(name = "annual_rate", precision = 7, scale = 3)
    private BigDecimal annualRate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "installments_count", nullable = false)
    private int installmentsCount;

    @Column(name = "first_due_date", nullable = false)
    private LocalDate firstDueDate;

    @Column(name = "contract_document_id")
    private UUID contractDocumentId;

    @Column(name = "bank_balance", precision = 19, scale = 4)
    private BigDecimal bankBalance;

    @Column(name = "bank_balance_date")
    private LocalDate bankBalanceDate;

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
