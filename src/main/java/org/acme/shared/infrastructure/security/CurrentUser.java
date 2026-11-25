package org.acme.shared.infrastructure.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.acme.domain.exceptions.NotAuthorizedException;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

/**
 * Usuario autenticado de la request actual, tomado del JWT validado (claim sub).
 * Los casos de uso reciben el userId desde acá, nunca desde un parámetro del cliente (SPEC §7.2).
 */
@RequestScoped
public class CurrentUser {

    @Inject
    JsonWebToken jwt;

    public UUID id() {
        String subject = jwt == null ? null : jwt.getSubject();
        if (subject == null) {
            throw new NotAuthorizedException();
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new NotAuthorizedException();
        }
    }
}
