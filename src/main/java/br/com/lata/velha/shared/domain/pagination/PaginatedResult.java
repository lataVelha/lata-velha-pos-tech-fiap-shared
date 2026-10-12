package br.com.lata.velha.shared.domain.pagination;

import java.util.List;
import java.util.function.Function;

public record PaginatedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T, R> PaginatedResult<R> map(PaginatedResult<T> result, Function<T, R> mapper) {
        return new PaginatedResult<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
