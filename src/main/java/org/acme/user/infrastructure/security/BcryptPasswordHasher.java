package org.acme.user.infrastructure.security;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.shared.application.AppSettings;
import org.acme.user.application.port.output.PasswordHasher;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.util.ModularCrypt;

import java.security.GeneralSecurityException;

/** Hash de contraseñas con bcrypt (Elytron). Formato Modular Crypt ($2a$...). */
@ApplicationScoped
public class BcryptPasswordHasher implements PasswordHasher {

    private static final WildFlyElytronPasswordProvider PROVIDER = new WildFlyElytronPasswordProvider();

    @Inject
    AppSettings settings;

    @Override
    public String hash(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword, settings.auth().bcryptCost());
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null) {
            return false;
        }
        try {
            Password decoded = ModularCrypt.decode(hash);
            PasswordFactory factory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, PROVIDER);
            BCryptPassword stored = (BCryptPassword) factory.translate(decoded);
            return factory.verify(stored, rawPassword.toCharArray());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
