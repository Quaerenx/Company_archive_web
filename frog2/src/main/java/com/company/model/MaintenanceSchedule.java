package com.company.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record MaintenanceSchedule(
        int intervalMonths,
        YearMonth anchorMonth,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean enabled) {
    private static final YearMonth DEFAULT_ANCHOR = YearMonth.of(2000, 1);

    public MaintenanceSchedule {
        if (intervalMonths != 1 && intervalMonths != 3) {
            throw new IllegalArgumentException(
                    "intervalMonths must be either 1 or 3");
        }
        anchorMonth = Objects.requireNonNull(anchorMonth, "anchorMonth");
        effectiveFrom = Objects.requireNonNull(
                effectiveFrom, "effectiveFrom");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException(
                    "effectiveTo must not be before effectiveFrom");
        }
    }

    public static MaintenanceSchedule monthlyDefault() {
        return new MaintenanceSchedule(
                1,
                DEFAULT_ANCHOR,
                DEFAULT_ANCHOR.atDay(1),
                null,
                true);
    }

    public boolean isDue(YearMonth targetMonth) {
        Objects.requireNonNull(targetMonth, "targetMonth");
        if (!enabled
                || targetMonth.isBefore(YearMonth.from(effectiveFrom))
                || (effectiveTo != null
                        && targetMonth.isAfter(YearMonth.from(effectiveTo)))) {
            return false;
        }
        long elapsedMonths = ChronoUnit.MONTHS.between(
                anchorMonth, targetMonth);
        return elapsedMonths >= 0 && elapsedMonths % intervalMonths == 0;
    }

    public boolean isQuarterly() {
        return intervalMonths == 3;
    }
}
