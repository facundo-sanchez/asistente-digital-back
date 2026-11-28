package org.acme.provider.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.provider.domain.model.Provider;
import org.acme.shared.application.model.PageQuery;

import java.util.Optional;
import java.util.UUID;

/**
 * Reconoce un proveedor por el nombre que leyó la IA: primero el nombre completo sin "S.A."/"SRL",
 * después su primera palabra ("Camuzzi Gas Pampeana S.A." → "Camuzzi"). Busca en el catálogo y en los propios.
 */
@ApplicationScoped
public class ProviderMatcher {

    private final ProviderQueryUseCase providers;

    @Inject
    public ProviderMatcher(ProviderQueryUseCase providers) {
        this.providers = providers;
    }

    public Optional<Provider> match(UUID userId, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String clean = name.replaceAll("(?i)\\b(s\\.?a\\.?|s\\.?r\\.?l\\.?|s\\.?a\\.?u\\.?)\\s*$", "").trim();
        Optional<Provider> full = first(userId, clean);
        if (full.isPresent()) {
            return full;
        }
        String firstWord = clean.split("\\s+")[0];
        return firstWord.length() >= 4 ? first(userId, firstWord) : Optional.empty();
    }

    private Optional<Provider> first(UUID userId, String text) {
        return providers.list(userId, text, null, new PageQuery(0, 1)).content().stream().findFirst();
    }
}
