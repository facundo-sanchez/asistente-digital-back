package org.acme.assistant.domain.model;

import java.util.UUID;

/** Un registro del usuario que la respuesta usó, para mostrarlo como link (CU-008 paso 7). */
public record Reference(ReferenceType type, UUID id, String label) {}
