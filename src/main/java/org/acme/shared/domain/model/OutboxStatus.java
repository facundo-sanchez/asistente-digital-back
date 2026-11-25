package org.acme.shared.domain.model;

public enum OutboxStatus {
    PENDING, PROCESSING, DONE, FAILED, DEAD
}
