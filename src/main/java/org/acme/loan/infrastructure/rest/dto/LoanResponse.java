package org.acme.loan.infrastructure.rest.dto;

import org.acme.loan.domain.model.LoanRateType;
import org.acme.obligation.infrastructure.rest.dto.ObligationResponse;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Un préstamo con lo que sale de sus cuotas. installments solo viene en el detalle (en la lista, vacío). */
@Schema(name = "LoanResponse")
public record LoanResponse(
        UUID id,
        String lender,
        String name,
        String loanNumber,
        String shortReference,
        String currency,
        BigDecimal principal,
        LoanRateType rateType,
        BigDecimal annualRate,
        LocalDate disbursementDate,
        int installmentsCount,
        LocalDate firstDueDate,
        UUID contractDocumentId,
        BigDecimal bankBalance,
        LocalDate bankBalanceDate,
        String notes,
        @Schema(description = "ACTIVE, PAID (sin cuotas pendientes) o CANCELLED") String status,
        String cancelReason,
        long paidInstallments,
        @Schema(description = "Suma de lo que falta de las cuotas pendientes (estimado hasta tener el importe real)") BigDecimal remaining,
        BigDecimal totalPaid,
        @Schema(description = "Próxima cuota pendiente; null si no hay") ObligationResponse next,
        @Schema(description = "Vencimiento de la última cuota") LocalDate lastDueDate,
        List<ObligationResponse> installments,
        @Schema(description = "Cronograma calculado (tasa fija): interés, capital, IVA, importe sin sellados y saldo de capital; vacío si no se puede")
        List<org.acme.loan.domain.model.ScheduleRow> schedule,
        Instant createdAt
) {}
