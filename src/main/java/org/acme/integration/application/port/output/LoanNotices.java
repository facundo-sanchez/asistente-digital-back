package org.acme.integration.application.port.output;

import org.acme.integration.application.model.LoanNotice;

import java.util.UUID;

/** Aplica un aviso de cuota al préstamo del usuario (lo implementa el módulo de préstamos). */
public interface LoanNotices {

    /** null si se aplicó; si no, el motivo para "Detectado en tu correo" (LOAN_NOT_FOUND, LOAN_AMBIGUOUS, LOAN_INSTALLMENT_PAID). */
    String apply(UUID userId, UUID providerId, LoanNotice notice);
}
