package org.acme.card.infrastructure.rest.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.card.application.usecase.CardTransactionUseCase;
import org.acme.card.infrastructure.rest.dto.CardMerchantRuleResponse;
import org.acme.card.infrastructure.rest.mapper.CardRestMapper;
import org.acme.shared.infrastructure.security.CurrentUser;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/** Rubros recordados por comercio (RN-019). */
@Path("/card-merchant-rules")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "JWT Auth")
@Tag(name = "Tarjetas")
public class CardMerchantRuleResource {

    @Inject
    CardTransactionUseCase transactions;

    @Inject
    CardRestMapper mapper;

    @Inject
    CurrentUser currentUser;

    @GET
    @Operation(summary = "Reglas por comercio")
    public List<CardMerchantRuleResponse> list() {
        return transactions.rules(currentUser.id()).stream().map(mapper::toResponse).toList();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Borrar una regla", description = "Los consumos ya clasificados no cambian")
    public void delete(@PathParam("id") UUID id) {
        transactions.deleteRule(currentUser.id(), id);
    }
}
