package org.acme.document.domain.model;

/** Estado de procesamiento de un documento (OCR / extracción IA). */
public enum ProcessingStatus {
    NOT_REQUIRED, PENDING, PROCESSING, COMPLETED, FAILED, REQUIRES_REVIEW
}
