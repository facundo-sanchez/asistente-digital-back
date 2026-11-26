package org.acme.card.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.acme.card.application.model.SpendingFilter;
import org.acme.card.application.usecase.CardReportUseCase;
import org.acme.card.application.usecase.CardTransactionUseCase;
import org.acme.card.domain.model.SpendingCategory;
import org.acme.card.infrastructure.rest.dto.CardTransactionResponse;
import org.acme.card.infrastructure.rest.dto.UpdateTransactionCategoryRequest;
import org.acme.card.infrastructure.rest.mapper.CardRestMapper;
import org.acme.shared.application.model.PageQuery;
import org.acme.shared.infrastructure.rest.dto.PageResponse;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.UUID;

/** Consumos con tarjeta (CU-016): el detalle del informe y corregir su rubro. */
@Path("/card-transactions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Tarjetas")
public class CardTransactionResource {

    @Inject
    CardTransactionUseCase transactions;

    @Inject
    CardReportUseCase report;

    @Inject
    CardRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Consumos del informe", description = "Meses de facturación from/to (yyyy-MM-dd); sin período, los últimos 6")
    public PageResponse<CardTransactionResponse> list(@QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to,
                                                      @QueryParam("cardId") UUID cardId, @QueryParam("category") SpendingCategory category,
                                                      @QueryParam("merchant") String merchant,
                                                      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("20") int size) {
        SpendingFilter filter = new SpendingFilter(from, to, cardId, category, merchant == null || merchant.isBlank() ? null : merchant);
        return PageResponse.from(report.transactions(currentUser.id(), filter, new PageQuery(page, size)), mapper::toResponse);
    }

    @PATCH
    @Path("/{id}/category")
    @Operation(summary = "Cambiar el rubro", description = "applyToMerchant=true lo recuerda para el comercio y lo aplica a sus otros consumos")
    public CardTransactionResponse recategorize(@PathParam("id") UUID id, @Valid @NotNull UpdateTransactionCategoryRequest request) {
        return mapper.toResponse(transactions.recategorize(currentUser.id(), id, request.category(), Boolean.TRUE.equals(request.applyToMerchant())));
    }
}
