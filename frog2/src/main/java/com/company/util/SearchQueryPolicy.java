package com.company.util;

public final class SearchQueryPolicy {
    public static final int MINIMUM_LENGTH = 2;
    public static final int MAXIMUM_LENGTH = 100;
    private static final String INVALID_LENGTH_MESSAGE =
            "검색어는 2자 이상 100자 이하로 입력해 주세요.";

    private SearchQueryPolicy() {
    }

    public static String normalize(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String normalized = query.trim();
        int length = normalized.codePointCount(0, normalized.length());
        if (length < MINIMUM_LENGTH || length > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException(INVALID_LENGTH_MESSAGE);
        }
        return normalized;
    }
}
