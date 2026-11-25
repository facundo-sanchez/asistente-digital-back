package org.acme.provider.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** El texto de búsqueda se usa literal en el LIKE. */
class ProviderRepositoryAdapterTest {

    @Test
    void escapaLosComodinesDelLike() {
        assertEquals("100\\%", ProviderRepositoryAdapter.escapeLike("100%"));
        assertEquals("a\\_b", ProviderRepositoryAdapter.escapeLike("a_b"));
        assertEquals("c\\\\d", ProviderRepositoryAdapter.escapeLike("c\\d"));
        assertEquals("coope", ProviderRepositoryAdapter.escapeLike("coope"));
    }
}
