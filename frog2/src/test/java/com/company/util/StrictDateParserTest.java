package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import org.junit.jupiter.api.Test;

class StrictDateParserTest {
    @Test
    void acceptsLeapDayAndPreservesDateText() throws Exception {
        java.util.Date date = StrictDateParser.parseDate("2024-02-29");

        assertNotNull(date);
        assertEquals("2024-02-29", StrictDateParser.formatDate(date));
        assertEquals(
                java.sql.Date.valueOf("2024-02-29"),
                StrictDateParser.parseSqlDateOrNull("2024-02-29"));
    }

    @Test
    void rejectsInvalidCalendarDateAndTrailingText() {
        assertThrows(
                ParseException.class,
                () -> StrictDateParser.parseDate("2026-02-30"));
        assertThrows(
                ParseException.class,
                () -> StrictDateParser.parseDate("2026-01-01extra"));
        assertNull(StrictDateParser.parseDateOrNull("2026-02-30"));
        assertNull(StrictDateParser.parseSqlDateOrNull("2026-02-30"));
    }

    @Test
    void timestampParserIsStrictAtDayAndTimeBoundaries() {
        assertNotNull(StrictDateParser.parseTimestampOrNull("2024-02-29T23:59"));
        assertNull(StrictDateParser.parseTimestampOrNull("2026-02-30T10:00"));
        assertNull(StrictDateParser.parseTimestampOrNull("2026-01-01T24:00"));
        assertNull(StrictDateParser.parseTimestampOrNull("2026-01-01T10:00extra"));
    }

    @Test
    void blankValuesRemainOptional() throws Exception {
        assertNull(StrictDateParser.parseDate("  "));
        assertNull(StrictDateParser.parseSqlDateOrNull(null));
        assertNull(StrictDateParser.parseTimestampOrNull(""));
        assertEquals("", StrictDateParser.formatDate(null));
    }
}
