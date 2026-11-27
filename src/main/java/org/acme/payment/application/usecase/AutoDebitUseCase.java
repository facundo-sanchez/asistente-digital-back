package org.acme.payment.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.obligation.application.port.output.ObligationRepository;
import org.acme.obligation.domain.model.EffectiveObligationStatus;
import org.acme.obligation.domain.model.Obligation;
import org.acme.obligation.domain.model.ObligationSummary;
import org.acme.payment.domain.model.Payment;
import org.acme.payment.domain.model.PaymentMethod;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;
import org.jboss.logging.Logger;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

/**
 * Débito automático (módulo 17): el día que vence, los vencimientos de servicios con débito automático se dan por
 * pagados con su saldo. Un vencimiento sin importe no se paga (no se inventan montos): queda para que el usuario lo cargue.
 */
@ApplicationScoped
public class AutoDebitUseCase {

    private static final Logger LOG = Logger.getLogger(AutoDebitUseCase.class);
    /** Si la tarea no corrió unos días, recupera los débitos de esos días; nunca más atrás. */
    static final int CATCH_UP_DAYS = org.acme.notification.application.usecase.AutoDebitWindow.DAYS;
    static final String NOTE = "Débito automático";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final UserRepository users;
    private final ServiceRepository services;
    private final ObligationRepository obligations;
    private final RegisterPaymentUseCase register;
    private final Clock clock;

    @Inject
    public AutoDebitUseCase(UserRepository users, ServiceRepository services, ObligationRepository obligations,
                            RegisterPaymentUseCase register, Clock clock) {
        this.users = users;
        this.services = services;
        this.obligations = obligations;
        this.register = register;
        this.clock = clock;
    }

    /** Devuelve cuántos pagos registró. */
    public int payDue() {
        // shortcut: recorre todos los usuarios activos, como los avisos; con miles, filtrar en la base
        return users.findActive().stream().mapToInt(this::payDueFor).sum();
    }

    int payDueFor(User user) {
        Set<UUID> autoDebit = services.findAutoDebitIds(user.getId());
        if (autoDebit.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now(clock.withZone(zone(user)));
        int paid = 0;
        for (ObligationSummary o : obligations.findDueBetween(user.getId(), today.minusDays(CATCH_UP_DAYS), today)) {
            Obligation obligation = o.obligation();
            if (!autoDebit.contains(obligation.getServiceId()) || !payable(o)) {
                continue;
            }
            try {
                register.register(user.getId(), Payment.builder()
                        .obligationId(obligation.getId())
                        .amount(o.balance())
                        .currency(obligation.getCurrency())
                        .paymentDate(obligation.getDueDate())
                        .paymentMethod(PaymentMethod.DIRECT_DEBIT)
                        .notes(NOTE)
                        .build());
                paid++;
            } catch (RuntimeException e) {
                // Un vencimiento que no se pudo pagar no frena a los demás
                LOG.errorf("No se pudo registrar el débito automático del vencimiento %s: %s", obligation.getId(), e.getMessage());
            }
        }
        return paid;
    }

    /** Pendiente, con importe y moneda: sin importe no se sabe cuánto se debitó. */
    static boolean payable(ObligationSummary o) {
        return o.effectiveStatus() != EffectiveObligationStatus.PAID && o.effectiveStatus() != EffectiveObligationStatus.CANCELLED
                && o.balance() != null && o.balance().signum() > 0 && o.obligation().getCurrency() != null;
    }

    private static ZoneId zone(User user) {
        try {
            return user.getTimezone() == null ? DEFAULT_ZONE : ZoneId.of(user.getTimezone());
        } catch (RuntimeException e) {
            return DEFAULT_ZONE;
        }
    }
}
