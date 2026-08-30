package com.company.util;

import java.util.Objects;

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

    public static String literalContainsLikePattern(String query) {
        String value = Objects.requireNonNull(query, "query");
        StringBuilder pattern = new StringBuilder(value.length() + 2);
        pattern.append('%');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '!' || character == '%' || character == '_') {
                pattern.append('!');
            }
            pattern.append(character);
        }
        return pattern.append('%').toString();
    }

    public static String literalContainsRegex(String query) {
        String value = Objects.requireNonNull(query, "query");
        StringBuilder pattern = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character)) {
                pattern.append("\\x{")
                        .append(Integer.toHexString(character))
                        .append('}');
                continue;
            }
            if ("\\.^$|?*+()[]{}#".indexOf(character) >= 0) {
                pattern.append('\\');
            }
            pattern.append(character);
        }
        return pattern.toString();
    }
}
