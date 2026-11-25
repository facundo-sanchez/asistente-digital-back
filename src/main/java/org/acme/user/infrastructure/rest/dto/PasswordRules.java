package org.acme.user.infrastructure.rest.dto;

/**
 * Política de contraseñas: 8 a 72 caracteres (72 = límite de bcrypt), al menos una letra y un número.
 * El frontend replica la misma regla en su validación.
 */
public final class PasswordRules {

    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d).+$";
    public static final String MESSAGE = "La contraseña debe incluir al menos una letra y un número";

    private PasswordRules() {
    }
}
