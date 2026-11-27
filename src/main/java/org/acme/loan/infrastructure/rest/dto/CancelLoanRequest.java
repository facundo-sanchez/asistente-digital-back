package org.acme.loan.infrastructure.rest.dto;

import jakarta.validation.constraints.Size;

public record CancelLoanRequest(@Size(max = 500) String reason) {}
