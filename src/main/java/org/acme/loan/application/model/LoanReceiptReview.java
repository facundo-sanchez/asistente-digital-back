package org.acme.loan.application.model;

import org.acme.document.domain.model.ExtractionValidationStatus;

import java.util.List;
import java.util.UUID;

/**
 * El comprobante de pago leído: el préstamo y la cuota que le corresponden (por número), y los avisos
 * (préstamo no cargado, cuota ya pagada, cantidad de cuotas distinta).
 */
public record LoanReceiptReview(UUID extractionId, UUID documentId, ExtractionValidationStatus status, ExtractedLoanReceipt receipt,
                                UUID suggestedLoanId, List<String> warnings) {}
