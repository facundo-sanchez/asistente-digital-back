package org.acme.integration.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Resultado de "Sincronizar ahora". */
@Schema(name = "SyncResponse")
public record SyncResponse(@Schema(description = "Documentos importados en esta sincronización") int imported, IntegrationResponse integration) {
}
