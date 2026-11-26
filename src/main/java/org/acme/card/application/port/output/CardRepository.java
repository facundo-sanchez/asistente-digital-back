package org.acme.card.application.port.output;

import org.acme.card.domain.model.Card;
import org.acme.card.domain.model.CardBrand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Tarjetas de un usuario. Toda consulta va filtrada por userId. */
public interface CardRepository {

    /** Inserta si no tiene id; si no, actualiza. */
    Card save(Card card);

    Optional<Card> findByIdForUser(UUID userId, UUID cardId);

    /** Por nombre. active null = todas. */
    List<Card> findForUser(UUID userId, Boolean active);

    /** La tarjeta del usuario con esa marca y terminación (son únicas por usuario). */
    Optional<Card> findByBrandAndLastFour(UUID userId, CardBrand brand, String lastFour);
}
