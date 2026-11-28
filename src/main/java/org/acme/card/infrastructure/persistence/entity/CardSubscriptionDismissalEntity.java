package org.acme.card.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Un comercio que el usuario marcó como "No es una suscripción". */
@Entity
@Table(name = "card_subscription_dismissals")
@Getter
@Setter
public class CardSubscriptionDismissalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "merchant", nullable = false)
    private String merchant;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
