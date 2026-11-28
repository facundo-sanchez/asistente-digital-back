package org.acme.invoice.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.invoice.application.port.output.InvoiceRepository;
import org.acme.invoice.application.port.output.PriceIncreaseAlerts;
import org.acme.invoice.domain.model.Invoice;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Alerta de aumentos (módulo 16): compara una factura recién confirmada con el promedio de las últimas 3 del mismo
 * servicio y moneda. Si sube más que el umbral del usuario, guarda el porcentaje en la factura y avisa.
 */
@ApplicationScoped
public class PriceIncreaseCheck {

    static final int HISTORY = 3;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final InvoiceRepository invoices;
    private final UserRepository users;
    private final PriceIncreaseAlerts alerts;

    @Inject
    public PriceIncreaseCheck(InvoiceRepository invoices, UserRepository users, PriceIncreaseAlerts alerts) {
        this.invoices = invoices;
        this.users = users;
        this.alerts = alerts;
    }

    /** Devuelve la factura con el porcentaje guardado si subió del umbral, o la misma factura si no. */
    public Invoice check(Invoice invoice, String serviceName, UUID obligationId) {
        if (invoice.getServiceId() == null || invoice.getTotalAmount() == null || invoice.getCurrency() == null || invoice.getDueDate() == null) {
            return invoice;
        }
        // Cargar una factura vieja cambia el historial de la siguiente: se recalcula sin avisar (no es una factura nueva)
        invoices.findNextConfirmed(invoice.getUserId(), invoice.getServiceId(), invoice.getCurrency(), invoice.getId(), invoice.getDueDate())
                .ifPresent(next -> {
                    BigDecimal percent = increase(next);
                    if (percent == null ? next.getPriceIncrease() != null : next.getPriceIncrease() == null || percent.compareTo(next.getPriceIncrease()) != 0) {
                        next.setPriceIncrease(percent);
                        invoices.save(next);
                    }
                });
        BigDecimal percent = increase(invoice);
        if (percent == null) {
            return invoice;
        }
        invoice.setPriceIncrease(percent);
        Invoice saved = invoices.save(invoice);
        alerts.increased(saved, serviceName, obligationId, percent, average(previous(invoice)));
        return saved;
    }

    /** El % sobre el promedio de las (hasta 3) anteriores por vencimiento, si llega al umbral del usuario; si no, null. */
    private BigDecimal increase(Invoice invoice) {
        List<Invoice> previous = previous(invoice);
        if (previous.isEmpty()) {
            return null;
        }
        BigDecimal percent = percentAbove(invoice.getTotalAmount(), average(previous));
        int threshold = users.findById(invoice.getUserId()).map(User::getPriceIncreaseThreshold).orElse(User.DEFAULT_PRICE_INCREASE_THRESHOLD);
        return percent == null || percent.compareTo(BigDecimal.valueOf(threshold)) < 0 ? null : percent;
    }

    /** Solo las que vencen antes: una factura no se compara con las posteriores. */
    private List<Invoice> previous(Invoice invoice) {
        return invoices.findLastConfirmed(invoice.getUserId(), invoice.getServiceId(), invoice.getCurrency(), invoice.getId(), invoice.getDueDate(), HISTORY);
    }

    private static BigDecimal average(List<Invoice> previous) {
        return previous.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(previous.size()), 2, RoundingMode.HALF_UP);
    }

    /** Cuánto está amount por encima de average, en % con un decimal; null si average es cero. */
    static BigDecimal percentAbove(BigDecimal amount, BigDecimal average) {
        if (average.signum() <= 0) {
            return null;
        }
        return amount.subtract(average).multiply(HUNDRED).divide(average, 1, RoundingMode.HALF_UP);
    }
}
