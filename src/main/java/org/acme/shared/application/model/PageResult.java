package org.acme.shared.application.model;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados, independiente de REST y de la base. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}
