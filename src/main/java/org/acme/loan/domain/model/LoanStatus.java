package org.acme.loan.domain.model;

/** ACTIVE mientras tenga cuotas; CANCELLED si se canceló (anticipadamente o por refinanciar). Pagado se calcula. */
public enum LoanStatus {
    ACTIVE,
    CANCELLED
}
