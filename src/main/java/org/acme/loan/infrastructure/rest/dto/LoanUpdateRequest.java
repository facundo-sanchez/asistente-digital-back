package org.acme.loan.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.acme.loan.domain.model.LoanRateType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Lo que se puede editar de un préstamo sin cambiar su cronograma. */
@Schema(name = "LoanUpdateRequest")
public record LoanUpdateRequest(
        @NotBlank(message = "Poné la entidad") @Size(max = 150) String lender,
        @NotBlank(message = "Poné un nombre") @Size(max = 150) String name,
        @Size(max = 60) String loanNumber,
        @Size(max = 60) String shortReference,
        @PositiveOrZero BigDecimal principal,
        LoanRateType rateType,
        @PositiveOrZero BigDecimal annualRate,
        LocalDate disbursementDate,
        UUID contractDocumentId,
        @Size(max = 2000) String notes
) {}
