package org.acme.integration.application.port.output;

import org.acme.integration.application.model.EmailOrigin;

import java.util.Optional;
import java.util.UUID;

/** Si un documento llegó por Gmail, de dónde. Quien llama ya verificó que el documento es del usuario. */
public interface EmailOriginRepository {

    Optional<EmailOrigin> findByDocument(UUID documentId);
}
