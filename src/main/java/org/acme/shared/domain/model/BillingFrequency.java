package org.acme.shared.domain.model;

/** Periodicidad de facturación / recurrencia. NONE no aplica a reglas de recurrencia. */
public enum BillingFrequency {
    NONE, WEEKLY, MONTHLY, BIMONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL
}
