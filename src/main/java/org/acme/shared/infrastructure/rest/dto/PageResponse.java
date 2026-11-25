package org.acme.shared.infrastructure.rest.dto;

import org.acme.shared.application.model.PageResult;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

/** Respuesta paginada genérica. page empieza en 0. */
public record PageResponse<T>(
        @Schema(description = "Elementos de la página") List<T> content,
        @Schema(description = "Número de página (base 0)", example = "0") int page,
        @Schema(description = "Tamaño de página", example = "20") int size,
        @Schema(description = "Total de elementos", example = "135") long totalElements,
        @Schema(description = "Total de páginas", example = "7") int totalPages
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    /** Convierte una página de la capa de aplicación mapeando cada elemento a su DTO. */
    public static <T, R> PageResponse<R> from(PageResult<T> result, Function<T, R> mapper) {
        return of(result.content().stream().map(mapper).toList(), result.page(), result.size(), result.totalElements());
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
