package com.company.model;

import java.sql.Date;
import java.time.LocalDate;

public final class MaintenanceHistoryFilter {
    static final int MIN_YEAR = 1900;
    static final int MAX_YEAR = 2100;
    static final int MAX_VERSION_LENGTH = 64;
    static final int MAX_QUERY_LENGTH = 120;

    private static final MaintenanceHistoryFilter EMPTY =
            new MaintenanceHistoryFilter(null, null, null);

    private final Integer year;
    private final String version;
    private final String query;

    private MaintenanceHistoryFilter(
            Integer year, String version, String query) {
        this.year = year;
        this.version = version;
        this.query = query;
    }

    public static MaintenanceHistoryFilter empty() {
        return EMPTY;
    }

    public static MaintenanceHistoryFilter parse(
            String rawYear, String rawVersion, String rawQuery) {
        String yearValue = trimToNull(rawYear);
        String version = trimToNull(rawVersion);
        String query = trimToNull(rawQuery);

        Integer year = null;
        if (yearValue != null) {
            try {
                year = Integer.valueOf(yearValue);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "History year must be numeric", exception);
            }
            if (year < MIN_YEAR || year > MAX_YEAR) {
                throw new IllegalArgumentException(
                        "History year is outside the supported range");
            }
        }
        if (version != null && version.length() > MAX_VERSION_LENGTH) {
            throw new IllegalArgumentException(
                    "History version filter is too long");
        }
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "History query filter is too long");
        }
        if (year == null && version == null && query == null) {
            return EMPTY;
        }
        return new MaintenanceHistoryFilter(year, version, query);
    }

    public Integer year() {
        return year;
    }

    public String version() {
        return version;
    }

    public String query() {
        return query;
    }

    public boolean hasFilters() {
        return year != null || version != null || query != null;
    }

    public Date yearStart() {
        return year == null
                ? null
                : Date.valueOf(LocalDate.of(year, 1, 1));
    }

    public Date yearEndExclusive() {
        return year == null
                ? null
                : Date.valueOf(LocalDate.of(year + 1, 1, 1));
    }

    public String versionLikePattern() {
        return likePattern(version);
    }

    public String queryLikePattern() {
        return likePattern(query);
    }

    private static String likePattern(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder(value.length() + 2);
        escaped.append('%');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '!' || character == '%' || character == '_') {
                escaped.append('!');
            }
            escaped.append(character);
        }
        return escaped.append('%').toString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
