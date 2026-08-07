package com.company.util;

public final class Pagination {
    private Pagination() {
    }

    public static int requestedPage(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            long page = Long.parseLong(value.trim());
            if (page < 1) {
                return 1;
            }
            return page > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) page;
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    public static int requestedPageSize(String value, int defaultSize, int maximumSize) {
        requirePageSize(defaultSize);
        requirePageSize(maximumSize);
        if (defaultSize > maximumSize) {
            throw new IllegalArgumentException("Default page size cannot exceed the maximum.");
        }
        if (value == null || value.isBlank()) {
            return defaultSize;
        }
        try {
            long pageSize = Long.parseLong(value.trim());
            if (pageSize < 1) {
                return defaultSize;
            }
            return (int) Math.min(pageSize, maximumSize);
        } catch (NumberFormatException exception) {
            return defaultSize;
        }
    }

    public static int totalPages(int totalCount, int pageSize) {
        if (totalCount < 0) {
            throw new IllegalArgumentException("Total count cannot be negative.");
        }
        requirePageSize(pageSize);
        return totalCount == 0 ? 0 : ((totalCount - 1) / pageSize) + 1;
    }

    public static int clampPage(int requestedPage, int totalPages) {
        if (totalPages <= 0) {
            return 1;
        }
        return Math.min(Math.max(requestedPage, 1), totalPages);
    }

    public static int offset(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be positive.");
        }
        requirePageSize(pageSize);
        return Math.multiplyExact(page - 1, pageSize);
    }

    private static void requirePageSize(int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be positive.");
        }
    }
}
