package org.acme.integration.application.model;

import java.util.UUID;

/** De qué proveedor vino por email un documento y a qué servicio se asoció al importarlo (null si a ninguno). */
public record EmailOrigin(UUID providerId, UUID serviceId) {
}
