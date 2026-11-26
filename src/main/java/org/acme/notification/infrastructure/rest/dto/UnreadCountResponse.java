package org.acme.notification.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "UnreadCountResponse")
public record UnreadCountResponse(long unread) {
}
