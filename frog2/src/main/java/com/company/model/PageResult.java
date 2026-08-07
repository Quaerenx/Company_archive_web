package com.company.model;

import com.company.util.Pagination;
import java.util.List;
import java.util.Objects;

public record PageResult<T>(
        List<T> items,
        int totalCount,
        int page,
        int pageSize) {

    public PageResult {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (totalCount < 0) {
            throw new IllegalArgumentException("Total count cannot be negative.");
        }
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Page values must be positive.");
        }
        int totalPages = Pagination.totalPages(totalCount, pageSize);
        if (totalPages > 0 && page > totalPages) {
            throw new IllegalArgumentException("Page cannot exceed total pages.");
        }
        if (totalCount == 0 && page != 1) {
            throw new IllegalArgumentException("An empty result must use page one.");
        }
    }

    public int totalPages() {
        return Pagination.totalPages(totalCount, pageSize);
    }
}
