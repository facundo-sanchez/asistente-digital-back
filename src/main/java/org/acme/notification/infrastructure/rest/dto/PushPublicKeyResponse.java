package org.acme.notification.infrastructure.rest.dto;

/** Clave pública VAPID: el navegador la necesita para suscribirse. */
public record PushPublicKeyResponse(String publicKey) {}
