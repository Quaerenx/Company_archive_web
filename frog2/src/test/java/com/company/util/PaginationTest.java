package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PaginationTest {
    @Test
    void normalizesRequestBoundaries() {
        assertEquals(1, Pagination.requestedPage(null));
        assertEquals(1, Pagination.requestedPage("0"));
        assertEquals(1, Pagination.requestedPage("not-a-number"));
        assertEquals(Integer.MAX_VALUE, Pagination.requestedPage("999999999999"));

        assertEquals(20, Pagination.requestedPageSize(null, 20, 100));
        assertEquals(20, Pagination.requestedPageSize("0", 20, 100));
        assertEquals(75, Pagination.requestedPageSize("75", 20, 100));
        assertEquals(100, Pagination.requestedPageSize("999", 20, 100));
    }

    @Test
    void calculatesAndClampsPagesWithoutSilentOverflow() {
        assertEquals(0, Pagination.totalPages(0, 20));
        assertEquals(3, Pagination.totalPages(41, 20));
        assertEquals(1, Pagination.clampPage(Integer.MAX_VALUE, 0));
        assertEquals(3, Pagination.clampPage(Integer.MAX_VALUE, 3));
        assertEquals(40, Pagination.offset(3, 20));
        assertThrows(
                ArithmeticException.class,
                () -> Pagination.offset(Integer.MAX_VALUE, 100));
    }
}
