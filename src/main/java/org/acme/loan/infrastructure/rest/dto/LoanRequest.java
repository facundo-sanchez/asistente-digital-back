package org.acme.loan.infrastructure.rest.dto;

import jakarta.validation.constraints.*;
import org.acme.loan.domain.model.LoanRateType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Alta de un préstamo: los datos del préstamo y del cronograma. */
@Schema(name = "LoanRequest")
public record LoanRequest(
        @NotBlank(message = "Poné la entidad") @Size(max = 150) @Schema(example = "BBVA") String lender,
        @NotBlank(message = "Poné un nombre") @Size(max = 150) @Schema(example = "Préstamo personal") String name,
        @Size(max = 60) @Schema(example = "00170280009600182032") String loanNumber,
        @Size(max = 60) @Schema(description = "Referencia corta de los avisos por email", example = "203/2") String shortReference,
        @NotNull(message = "Elegí la moneda") @Pattern(regexp = "ARS|USD", message = "La moneda tiene que ser ARS o USD") String currency,
        @PositiveOrZero @Schema(description = "Capital pedido", example = "7000000") BigDecimal principal,
        LoanRateType rateType,
        @PositiveOrZero @Schema(description = "TNA en %", example = "71") BigDecimal annualRate,
        LocalDate disbursementDate,
        @Min(value = 1, message = "Al menos 1 cuota") @Max(value = 360, message = "Hasta 360 cuotas") int installmentsCount,
        @NotNull(message = "Poné el importe de la cuota") @Positive(message = "El importe tiene que ser mayor a cero")
        @Schema(example = "584292.14") BigDecimal installmentAmount,
        @NotNull(message = "Poné la fecha de la primera cuota") LocalDate firstDueDate,
        @PositiveOrZero @Schema(description = "Cuotas ya pagadas antes de cargarlo") int paidInstallments,
        UUID contractDocumentId,
        @Size(max = 2000) String notes,
        @Schema(description = "Propuesta del comprobante de alta leído con IA, si se cargó desde ahí") UUID extractionId
) {}
