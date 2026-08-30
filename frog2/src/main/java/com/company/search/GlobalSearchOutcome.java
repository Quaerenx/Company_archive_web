package com.company.search;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record GlobalSearchOutcome(
        List<GlobalSearchResult> results,
        List<String> unavailableCategories) {
    private static final int SOURCE_COUNT = 5;

    public GlobalSearchOutcome {
        results = List.copyOf(Objects.requireNonNull(results, "results"));
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        for (String category : Objects.requireNonNull(
                unavailableCategories, "unavailableCategories")) {
            if (category != null && !category.isBlank()) {
                categories.add(category.strip());
            }
        }
        if (categories.size() > SOURCE_COUNT) {
            throw new IllegalArgumentException(
                    "Unavailable category count exceeds search sources");
        }
        unavailableCategories = List.copyOf(categories);
    }

    public boolean partial() {
        return !unavailableCategories.isEmpty();
    }

    public boolean allSourcesUnavailable() {
        return unavailableCategories.size() == SOURCE_COUNT;
    }
}
