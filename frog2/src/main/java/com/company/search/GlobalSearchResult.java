package com.company.search;

import java.util.List;
import java.util.Objects;

public record GlobalSearchResult(
        String category,
        String label,
        String description,
        String path,
        String groupCode,
        String morePath,
        List<GlobalSearchAction> actions) {
    public GlobalSearchResult(
            String category,
            String label,
            String description,
            String path) {
        this(category, label, description, path, category, null, List.of());
    }

    public GlobalSearchResult {
        category = requireText(category, "category");
        label = requireText(label, "label");
        description = description == null ? "" : description.strip();
        path = requireText(path, "path");
        if (!path.startsWith("/") || path.startsWith("//")) {
            throw new IllegalArgumentException(
                    "Search result path must be context-relative");
        }
        groupCode = requireText(groupCode, "groupCode");
        if (morePath != null) {
            morePath = morePath.strip();
            if (morePath.isEmpty()
                    || !morePath.startsWith("/")
                    || morePath.startsWith("//")) {
                throw new IllegalArgumentException(
                        "Search result morePath must be context-relative");
            }
        }
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
