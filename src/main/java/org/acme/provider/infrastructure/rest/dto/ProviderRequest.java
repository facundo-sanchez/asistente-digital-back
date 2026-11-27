package org.acme.provider.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.acme.shared.domain.model.ServiceCategory;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Alta de un proveedor propio del usuario (cuando no está en el catálogo). */
@Schema(name = "ProviderRequest")
public record ProviderRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio")
        @Size(max = 150)
        @Schema(example = "Cooperativa de Agua Local") String name,

        @NotNull(message = "La categoría es obligatoria")
        ServiceCategory category,

        @Size(max = 500)
        @Pattern(regexp = "^https?://\\S+$", message = "El sitio web tiene que empezar con http:// o https://")
        @Schema(example = "https://www.aguax.com.ar") String website,

        @Size(max = 200)
        @Pattern(regexp = "^\\s*$|^\\s*([^@\\s]+@)?[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+\\s*$",
                message = "Poné una dirección de email (info@empresa.com.ar) o un dominio (empresa.com.ar)")
        @Schema(description = "Desde dónde llegan sus facturas: dirección o dominio. Gmail las detecta con esto",
                example = "info@nativaseguros.com.ar") String invoiceSender
) {}
