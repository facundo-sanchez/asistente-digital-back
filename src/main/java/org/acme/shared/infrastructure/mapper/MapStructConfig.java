package org.acme.shared.infrastructure.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Configuración común de MapStruct.
 * - Los mappers son beans CDI inyectables en Quarkus.
 * - Un campo destino sin mapear es error de compilación: obliga a mantener
 *   entity, model y DTO sincronizados.
 */
@MapperConfig(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MapStructConfig {
}
