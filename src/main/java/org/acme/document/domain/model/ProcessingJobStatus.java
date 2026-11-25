package org.acme.document.domain.model;

/** Estado de un trabajo de procesamiento. DEAD = superó el máximo de intentos. */
public enum ProcessingJobStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, REQUIRES_REVIEW, DEAD
}
