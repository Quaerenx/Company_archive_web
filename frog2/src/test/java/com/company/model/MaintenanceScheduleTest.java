package com.company.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class MaintenanceScheduleTest {
    @Test
    void monthlyDefaultIsDueEveryMonth() {
        MaintenanceSchedule schedule = MaintenanceSchedule.monthlyDefault();

        assertTrue(schedule.isDue(YearMonth.of(2026, 7)));
        assertTrue(schedule.isDue(YearMonth.of(2026, 8)));
        assertFalse(schedule.isQuarterly());
    }

    @Test
    void quarterlyScheduleUsesEachCustomersOwnAnchorMonth() {
        MaintenanceSchedule februaryAnchor = new MaintenanceSchedule(
                3,
                YearMonth.of(2025, 2),
                LocalDate.of(2025, 2, 1),
                null,
                true);

        assertTrue(februaryAnchor.isDue(YearMonth.of(2026, 8)));
        assertFalse(februaryAnchor.isDue(YearMonth.of(2026, 7)));
        assertTrue(februaryAnchor.isQuarterly());
    }

    @Test
    void marchAnchorIsDueInMarchJuneSeptemberAndDecember() {
        MaintenanceSchedule marchAnchor = new MaintenanceSchedule(
                3,
                YearMonth.of(2000, 3),
                LocalDate.of(2000, 1, 1),
                null,
                true);

        for (int month = 1; month <= 12; month++) {
            boolean expected = month == 3 || month == 6 || month == 9 || month == 12;
            if (expected) {
                assertTrue(marchAnchor.isDue(YearMonth.of(2026, month)));
            } else {
                assertFalse(marchAnchor.isDue(YearMonth.of(2026, month)));
            }
        }
    }

    @Test
    void disabledOrOutOfEffectiveRangeScheduleIsNotDue() {
        MaintenanceSchedule disabled = new MaintenanceSchedule(
                3,
                YearMonth.of(2026, 2),
                LocalDate.of(2026, 2, 1),
                null,
                false);
        MaintenanceSchedule expired = new MaintenanceSchedule(
                1,
                YearMonth.of(2025, 1),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 7, 31),
                true);

        assertFalse(disabled.isDue(YearMonth.of(2026, 8)));
        assertFalse(expired.isDue(YearMonth.of(2026, 8)));
    }

    @Test
    void decemberQuarterlyAnchorContinuesAcrossTheYearBoundary() {
        MaintenanceSchedule decemberAnchor = new MaintenanceSchedule(
                3,
                YearMonth.of(2025, 12),
                LocalDate.of(2025, 12, 1),
                null,
                true);

        assertTrue(decemberAnchor.isDue(YearMonth.of(2025, 12)));
        assertFalse(decemberAnchor.isDue(YearMonth.of(2026, 1)));
        assertFalse(decemberAnchor.isDue(YearMonth.of(2026, 2)));
        assertTrue(decemberAnchor.isDue(YearMonth.of(2026, 3)));
        assertTrue(decemberAnchor.isDue(YearMonth.of(2026, 12)));
    }

    @Test
    void leapYearFebruaryUsesMonthResidueRatherThanDayCount() {
        MaintenanceSchedule februaryAnchor = new MaintenanceSchedule(
                3,
                YearMonth.of(2024, 2),
                LocalDate.of(2024, 2, 29),
                null,
                true);

        assertTrue(februaryAnchor.isDue(YearMonth.of(2024, 2)));
        assertFalse(februaryAnchor.isDue(YearMonth.of(2024, 3)));
        assertTrue(februaryAnchor.isDue(YearMonth.of(2024, 5)));
        assertTrue(februaryAnchor.isDue(YearMonth.of(2025, 2)));
    }
}
