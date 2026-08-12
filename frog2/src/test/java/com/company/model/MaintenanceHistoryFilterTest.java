package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import org.junit.jupiter.api.Test;

class MaintenanceHistoryFilterTest {
    @Test
    void blankValuesProduceAnEmptyFilter() {
        MaintenanceHistoryFilter filter =
                MaintenanceHistoryFilter.parse("  ", null, "\t");

        assertNull(filter.year());
        assertNull(filter.version());
        assertNull(filter.query());
        assertFalse(filter.hasFilters());
    }

    @Test
    void normalizesValuesAndBuildsAnExclusiveYearRange() {
        MaintenanceHistoryFilter filter =
                MaintenanceHistoryFilter.parse(
                        " 2026 ", " 23.4.0-13 ", " 김동완 ");

        assertEquals(2026, filter.year());
        assertEquals("23.4.0-13", filter.version());
        assertEquals("김동완", filter.query());
        assertEquals(Date.valueOf("2026-01-01"), filter.yearStart());
        assertEquals(Date.valueOf("2027-01-01"), filter.yearEndExclusive());
        assertTrue(filter.hasFilters());
    }

    @Test
    void escapesLiteralLikeMetacharacters() {
        MaintenanceHistoryFilter filter =
                MaintenanceHistoryFilter.parse(null, "v_1%", "error_%!");

        assertEquals("%v!_1!%%", filter.versionLikePattern());
        assertEquals("%error!_!%!!%", filter.queryLikePattern());
    }

    @Test
    void rejectsInvalidYearAndOverlongText() {
        assertThrows(IllegalArgumentException.class,
                () -> MaintenanceHistoryFilter.parse("twenty", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> MaintenanceHistoryFilter.parse("1899", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> MaintenanceHistoryFilter.parse("2101", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> MaintenanceHistoryFilter.parse(
                        null, "v".repeat(65), null));
        assertThrows(IllegalArgumentException.class,
                () -> MaintenanceHistoryFilter.parse(
                        null, null, "q".repeat(121)));
    }
}
