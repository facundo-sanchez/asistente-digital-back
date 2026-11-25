package org.acme.user.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.user.application.model.UpdateProfileCommand;
import org.acme.user.application.port.output.UserRepository;
import org.acme.user.domain.model.User;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

/** Perfil del usuario autenticado (/users/me). */
@ApplicationScoped
public class ProfileUseCase {

    private final UserRepository users;

    @Inject
    public ProfileUseCase(UserRepository users) {
        this.users = users;
    }

    public User get(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("usuario"));
    }

    @Transactional
    public User update(UUID userId, UpdateProfileCommand command) {
        validate(command);
        User user = get(userId);
        user.setName(command.name().trim());
        user.setTimezone(command.timezone());
        user.setLocale(command.locale());
        user.setDefaultCurrency(command.defaultCurrency());
        return users.save(user);
    }

    private static void validate(UpdateProfileCommand command) {
        try {
            ZoneId.of(command.timezone());
        } catch (DateTimeException e) {
            throw new ServiceException("Zona horaria inválida: " + command.timezone());
        }
        if (Locale.forLanguageTag(command.locale()).getLanguage().isEmpty()) {
            throw new ServiceException("Configuración regional inválida: " + command.locale());
        }
        try {
            Currency.getInstance(command.defaultCurrency());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Moneda inválida: " + command.defaultCurrency());
        }
    }
}
