package org.acme.card.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.card.application.model.CardView;
import org.acme.card.application.port.output.CardRepository;
import org.acme.card.domain.model.Card;
import org.acme.domain.exceptions.ConflictException;
import org.acme.domain.exceptions.NotFoundException;
import org.acme.domain.exceptions.ServiceException;
import org.acme.service.application.port.output.ServiceRepository;
import org.acme.service.domain.model.Service;
import org.acme.shared.domain.model.ServiceCategory;

import java.util.List;
import java.util.UUID;

/**
 * Tarjetas de crédito (CU-015): alta, edición, consulta y baja lógica. Cierre y vencimiento no
 * se guardan en la tarjeta: los fija el banco en cada resumen.
 */
@ApplicationScoped
public class ManageCardUseCase {

    static final String UNKNOWN_SERVICE = "El servicio elegido no existe";
    static final String NOT_A_CARD_SERVICE = "Elegí un servicio de la categoría Tarjetas (el banco o emisor)";
    static final String DUPLICATE = "Ya tenés una %s terminada en %s";

    private final CardRepository cards;
    private final ServiceRepository services;

    @Inject
    public ManageCardUseCase(CardRepository cards, ServiceRepository services) {
        this.cards = cards;
        this.services = services;
    }

    @Transactional
    public CardView register(UUID userId, Card draft) {
        String serviceName = validate(userId, draft, null);
        Card card = cards.save(clean(draft).toBuilder().id(null).userId(userId).active(true).build());
        return new CardView(card, serviceName);
    }

    @Transactional
    public CardView update(UUID userId, UUID cardId, Card draft) {
        Card card = find(userId, cardId);
        String serviceName = validate(userId, draft, card.getId());
        Card changes = clean(draft);
        card.setServiceId(changes.getServiceId());
        card.setName(changes.getName());
        card.setBrand(changes.getBrand());
        card.setLastFour(changes.getLastFour());
        card.setAccountLastFour(changes.getAccountLastFour());
        card.setHolderName(changes.getHolderName());
        card.setAdditional(changes.isAdditional());
        card.setCreditLimit(changes.getCreditLimit());
        return new CardView(cards.save(card), serviceName);
    }

    /** Desactivar no borra: sus resúmenes y vencimientos se conservan. */
    @Transactional
    public CardView changeStatus(UUID userId, UUID cardId, boolean active) {
        Card card = find(userId, cardId);
        card.setActive(active);
        return view(userId, cards.save(card));
    }

    public CardView get(UUID userId, UUID cardId) {
        return view(userId, find(userId, cardId));
    }

    public List<CardView> list(UUID userId, Boolean active) {
        List<Card> found = cards.findForUser(userId, active);
        var names = services.findNames(userId, found.stream().map(Card::getServiceId).distinct().toList());
        return found.stream().map(card -> new CardView(card, names.get(card.getServiceId()))).toList();
    }

    /** Devuelve el nombre del servicio. keepId: al editar, la propia tarjeta no cuenta como repetida. */
    private String validate(UUID userId, Card draft, UUID keepId) {
        // Un servicio de otro usuario responde igual que uno inexistente (RN-002)
        Service service = services.findByIdForUser(userId, draft.getServiceId())
                .orElseThrow(() -> new ServiceException(UNKNOWN_SERVICE));
        if (service.getCategory() != ServiceCategory.CREDIT_CARD) {
            throw new ServiceException(NOT_A_CARD_SERVICE);
        }
        if (draft.getLastFour() != null) {
            cards.findByBrandAndLastFour(userId, draft.getBrand(), draft.getLastFour())
                    .filter(existing -> !existing.getId().equals(keepId))
                    .ifPresent(existing -> {
                        throw new ConflictException(DUPLICATE.formatted(existing.getName(), draft.getLastFour()));
                    });
        }
        return service.getName();
    }

    private static Card clean(Card draft) {
        return draft.toBuilder()
                .name(draft.getName().trim())
                .holderName(draft.getHolderName() == null || draft.getHolderName().isBlank() ? null : draft.getHolderName().trim())
                .build();
    }

    private CardView view(UUID userId, Card card) {
        return new CardView(card, services.findNames(userId, List.of(card.getServiceId())).get(card.getServiceId()));
    }

    /** Una tarjeta de otro usuario responde igual que una inexistente. */
    private Card find(UUID userId, UUID cardId) {
        return cards.findByIdForUser(userId, cardId).orElseThrow(() -> new NotFoundException("tarjeta"));
    }
}
