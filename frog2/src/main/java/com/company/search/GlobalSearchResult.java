package com.company.search;

import java.util.Objects;

public record GlobalSearchResult(
        String category,
        String label,
        String description,
        String path) {
    public GlobalSearchResult {
        category = requireText(category, "category");
        label = requireText(label, "label");
        description = description == null ? "" : description.strip();
        path = requireText(path, "path");
        if (!path.startsWith("/") || path.startsWith("//")) {
            throw new IllegalArgumentException(
                    "Search result path must be context-relative");
        }
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
