package com.company.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Supplies calendar dates used by the customer-support business screens.
 */
public final class BusinessDate {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private BusinessDate() {
    }

    public static Clock systemClock() {
        return Clock.system(ZONE);
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(inBusinessZone(clock));
    }

    public static YearMonth currentMonth(Clock clock) {
        return YearMonth.from(today(clock));
    }

    private static Clock inBusinessZone(Clock clock) {
        return Objects.requireNonNull(clock, "clock").withZone(ZONE);
    }
}
