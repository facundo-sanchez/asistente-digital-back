package org.acme.service.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.acme.shared.domain.model.BillingFrequency;
import org.acme.shared.domain.model.ServiceCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Schema(name = "ServiceRequest")
public record ServiceRequest(
        @NotNull(message = "El proveedor es obligatorio")
        UUID providerId,

        @NotBlank(message = "El nombre del servicio es obligatorio")
        @Size(max = 150)
        @Schema(example = "Luz casa") String name,

        @Size(max = 150)
        @Schema(description = "Nro. de cliente / socio / suministro", example = "123456") String customerReference,

        @NotNull(message = "La categoría es obligatoria")
        ServiceCategory category,

        @NotNull(message = "La periodicidad es obligatoria")
        BillingFrequency billingFrequency,

        LocalDate startDate,
        LocalDate endDate,

        @Size(max = 2000, message = "Las notas admiten hasta 2000 caracteres")
        String notes,

        @Size(max = 500)
        @Pattern(regexp = "\\s*(https://\\S+)?\\s*", message = "Ingresá un link que empiece con https://")
        @Schema(description = "Página donde se pagan sus facturas", example = "https://www.camuzzigas.com.ar") String paymentUrl,

        @Schema(description = "Se paga por débito automático") boolean autoDebit,

        @Size(max = 100)
        @Schema(description = "Con qué se debita", example = "Visa BBVA") String autoDebitMethod,

        @Schema(description = "Datos adicionales libres (opcional)")
        Map<String, Object> metadata
) {}
