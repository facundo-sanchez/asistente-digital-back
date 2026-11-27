package org.acme.loan.application.model;

import org.acme.document.domain.model.ExtractionValidationStatus;

import java.util.List;
import java.util.UUID;

/** El comprobante de alta leído, para completar el alta del préstamo. */
public record LoanContractReview(UUID extractionId, UUID documentId, ExtractionValidationStatus status, ExtractedLoanContract contract,
                                 List<String> warnings) {}
