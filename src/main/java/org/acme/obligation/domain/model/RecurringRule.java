package org.acme.obligation.domain.model;

import lombok.*;
import org.acme.shared.domain.model.BillingFrequency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Regla de recurrencia (CU-014): genera un vencimiento por período (alquiler, seguros, VTV).
 * nextExpectedDate es el vencimiento del próximo período que todavía no se generó.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RecurringRule {
    private UUID id;
    private UUID userId;
    private UUID serviceId;
    private UUID assetId;
    private String title;
    /** Tipo de los vencimientos que genera. */
    @Builder.Default
    private ObligationType obligationType = ObligationType.OTHER;
    /** NONE no es válido para una regla. */
    private BillingFrequency frequency;
    private Integer dayOfMonth;
    private LocalDate nextExpectedDate;
    private BigDecimal estimatedAmount;
    private String currency;
    @Builder.Default
    private RecurrenceOrigin origin = RecurrenceOrigin.USER;
    @Builder.Default
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", ES_AR);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy", ES_AR);

    /** Vencimiento del período siguiente a date. dayOfMonth 31 cae en el último día de los meses cortos. */
    public LocalDate following(LocalDate date) {
        LocalDate next = switch (frequency) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case BIMONTHLY -> date.plusMonths(2);
            case QUARTERLY -> date.plusMonths(3);
            case SEMIANNUAL -> date.plusMonths(6);
            case ANNUAL -> date.plusYears(1);
            case NONE -> throw new IllegalStateException("Una regla no puede tener periodicidad NONE");
        };
        if (frequency == BillingFrequency.WEEKLY || dayOfMonth == null) {
            return next;
        }
        // plusMonths ya recortó un 31 a 30/28; acá se vuelve al día pedido si el mes lo tiene
        return next.withDayOfMonth(Math.min(dayOfMonth, next.lengthOfMonth()));
    }

    /** El vencimiento del período pendiente (nextExpectedDate). No avanza la regla. */
    public Obligation nextObligation() {
        String period = frequency == BillingFrequency.WEEKLY ? nextExpectedDate.format(DAY) : nextExpectedDate.format(MONTH);
        return Obligation.builder()
                .userId(userId)
                .serviceId(serviceId)
                .recurrenceId(id)
                .title(title + " · " + period)
                .obligationType(obligationType)
                .amount(estimatedAmount)
                .currency(currency)
                .dueDate(nextExpectedDate)
                // El importe lo fija el usuario (no es una estimación sin factura): se puede pagar
                .estimated(false)
                .build();
    }

    /** Pasa al período siguiente. */
    public void advance() {
        nextExpectedDate = following(nextExpectedDate);
    }
}
