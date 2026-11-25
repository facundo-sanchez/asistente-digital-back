package org.acme.payment.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.document.application.port.output.DocumentRepository;
import org.acme.domain.exceptions.ServiceException;
import org.acme.obligation.domain.model.Obligation;
import org.acme.payment.domain.model.Payment;

import java.util.Set;
import java.util.UUID;

/** Reglas comunes al registro y a la corrección de un pago. */
@ApplicationScoped
public class PaymentDraftValidator {

    static final String WRONG_CURRENCY = "El pago tiene que ser en %s, la moneda de la obligación";
    static final String INVALID_CURRENCY = "La moneda tiene que ser ARS o USD";
    static final String UNKNOWN_RECEIPT = "El comprobante elegido no existe";

    private static final Set<String> CURRENCIES = Set.of("ARS", "USD");

    private final DocumentRepository documents;

    @Inject
    public PaymentDraftValidator(DocumentRepository documents) {
        this.documents = documents;
    }

    public void validate(UUID userId, Obligation obligation, Payment draft) {
        if (obligation.getCurrency() != null && !obligation.getCurrency().equals(draft.getCurrency())) {
            throw new ServiceException(WRONG_CURRENCY.formatted(obligation.getCurrency()));
        }
        if (!CURRENCIES.contains(draft.getCurrency())) {
            throw new ServiceException(INVALID_CURRENCY);
        }
        // Un documento de otro usuario o borrado responde igual que uno inexistente
        if (draft.getReceiptDocumentId() != null && documents.findByIdForUser(userId, draft.getReceiptDocumentId()).isEmpty()) {
            throw new ServiceException(UNKNOWN_RECEIPT);
        }
    }

    /** El pago listo para guardar: del usuario, de esa obligación y sin notas vacías. */
    public static Payment clean(UUID userId, UUID obligationId, Payment draft) {
        return draft.toBuilder()
                .id(null)
                .userId(userId)
                .obligationId(obligationId)
                .voidedAt(null)
                .voidReason(null)
                .notes(draft.getNotes() == null || draft.getNotes().isBlank() ? null : draft.getNotes().trim())
                .build();
    }
}
