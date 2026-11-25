package org.acme.user.application.usecase;

import org.acme.user.application.port.output.PasswordHasher;

/** Hasher determinista para tests: "h:" + contraseña. Cuenta las verificaciones. */
public class FakePasswordHasher implements PasswordHasher {

    private int verifications;

    public int verifications() {
        return verifications;
    }

    @Override
    public String hash(String rawPassword) {
        return "h:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        verifications++;
        return hash != null && hash.equals("h:" + rawPassword);
    }
}
