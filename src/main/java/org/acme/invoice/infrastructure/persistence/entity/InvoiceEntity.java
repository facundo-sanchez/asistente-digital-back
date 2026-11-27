package org.acme.invoice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.acme.invoice.domain.model.InvoiceSource;
import org.acme.invoice.domain.model.InvoiceStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class InvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "extraction_result_id")
    private UUID extractionResultId;

    @Column(name = "invoice_number", length = 120)
    private String invoiceNumber;

    @Column(name = "billing_period", length = 30)
    private String billingPeriod;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    // CHAR(3) en la base: sin esto Hibernate valida la columna como VARCHAR
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "payment_code", length = 200)
    private String paymentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private InvoiceSource source;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "price_increase", precision = 7, scale = 1)
    private BigDecimal priceIncrease;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
