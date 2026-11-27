package org.acme.assistant.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.card.application.usecase.CardReportUseCase;
import org.acme.dashboard.application.usecase.DashboardUseCase;
import org.acme.document.application.usecase.DocumentQueryUseCase;
import org.acme.invoice.application.usecase.InvoiceQueryUseCase;
import org.acme.obligation.application.usecase.ObligationQueryUseCase;
import org.acme.service.application.usecase.ServiceQueryUseCase;

import java.time.LocalDate;
import java.util.UUID;

/** Los casos de uso que consultan las herramientas. Arma las herramientas de un usuario para una respuesta. */
@ApplicationScoped
public class AssistantToolbox {

    final ObligationQueryUseCase obligations;
    final InvoiceQueryUseCase invoices;
    final ServiceQueryUseCase services;
    final DashboardUseCase dashboard;
    final CardReportUseCase cards;
    final DocumentQueryUseCase documents;
    final ObjectMapper json;
    final org.acme.loan.application.usecase.LoanUseCase loans;

    @Inject
    public AssistantToolbox(ObligationQueryUseCase obligations, InvoiceQueryUseCase invoices, ServiceQueryUseCase services,
                            DashboardUseCase dashboard, CardReportUseCase cards, DocumentQueryUseCase documents, ObjectMapper json,
                            org.acme.loan.application.usecase.LoanUseCase loans) {
        this.obligations = obligations;
        this.invoices = invoices;
        this.services = services;
        this.dashboard = dashboard;
        this.cards = cards;
        this.documents = documents;
        this.json = json;
        this.loans = loans;
    }

    public AssistantTools forUser(UUID userId, LocalDate today) {
        return new AssistantTools(this, userId, today);
    }
}
