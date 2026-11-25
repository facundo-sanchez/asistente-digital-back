package org.acme.shared.domain.model;

/** Email a enviar. body en texto plano. */
public record EmailMessage(String to, String subject, String body) {}
