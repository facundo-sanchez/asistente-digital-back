package org.acme.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_subscriptions")
@Getter
@Setter
public class PushSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "endpoint", nullable = false, unique = true)
    private String endpoint;

    @Column(name = "p256dh", nullable = false, length = 200)
    private String p256dh;

    @Column(name = "auth", nullable = false, length = 100)
    private String auth;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
