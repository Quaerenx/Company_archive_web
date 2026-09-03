package com.company.search;

import java.util.Objects;

public record GlobalSearchAction(String label, String path) {
    public GlobalSearchAction {
        label = requireText(label, "label");
        path = requireText(path, "path");
        if (!path.startsWith("/") || path.startsWith("//")) {
            throw new IllegalArgumentException(
                    "Search action path must be context-relative");
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
