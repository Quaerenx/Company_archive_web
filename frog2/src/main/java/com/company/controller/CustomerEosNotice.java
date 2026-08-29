package com.company.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

public record CustomerEosNotice(long daysRemaining, String tone, String message) {
    public long getDaysRemaining() {
        return daysRemaining;
    }

    public String getTone() {
        return tone;
    }

    public String getMessage() {
        return message;
    }

    static CustomerEosNotice from(Date eosDate, LocalDate today) {
        Objects.requireNonNull(eosDate, "eosDate");
        Objects.requireNonNull(today, "today");
        LocalDate eosLocalDate = eosDate instanceof java.sql.Date sqlDate
                ? sqlDate.toLocalDate()
                : eosDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
        long days = ChronoUnit.DAYS.between(today, eosLocalDate);
        String tone = days <= 30 ? "danger"
                : days <= 90 ? "warning"
                : "neutral";
        String message;
        if (days < 0) {
            message = "Vertica EOS가 " + Math.abs(days) + "일 지났습니다";
        } else if (days == 0) {
            message = "Vertica EOS가 오늘입니다";
        } else {
            message = "Vertica EOS까지 " + days + "일 남았습니다";
        }
        return new CustomerEosNotice(days, tone, message);
    }
}
