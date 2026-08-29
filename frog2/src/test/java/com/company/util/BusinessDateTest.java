package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BusinessDateTest {
    @Test
    void seoulBusinessDateCrossesMidnightAndMonthBeforeUtc() {
        Clock beforeMidnight = Clock.fixed(
                Instant.parse("2026-08-31T14:59:59.999999999Z"),
                ZoneOffset.UTC);
        Clock atMidnight = Clock.fixed(
                Instant.parse("2026-08-31T15:00:00Z"),
                ZoneOffset.UTC);

        assertEquals(LocalDate.of(2026, 8, 31),
                BusinessDate.today(beforeMidnight));
        assertEquals(YearMonth.of(2026, 8),
                BusinessDate.currentMonth(beforeMidnight));
        assertEquals(LocalDate.of(2026, 9, 1),
                BusinessDate.today(atMidnight));
        assertEquals(YearMonth.of(2026, 9),
                BusinessDate.currentMonth(atMidnight));
    }
}
