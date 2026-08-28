package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CustomerEosNoticeTest {
    @Test
    void assignsUrgencyFromRemainingDays() {
        LocalDate today = LocalDate.of(2026, 8, 27);

        CustomerEosNotice warning = CustomerEosNotice.from(
                Date.valueOf("2026-10-31"), today);
        CustomerEosNotice danger = CustomerEosNotice.from(
                Date.valueOf("2026-09-10"), today);
        CustomerEosNotice neutral = CustomerEosNotice.from(
                Date.valueOf("2027-01-31"), today);

        assertEquals(65, warning.daysRemaining());
        assertEquals("warning", warning.tone());
        assertEquals("Vertica EOS까지 65일 남았습니다", warning.message());
        assertEquals("danger", danger.tone());
        assertEquals("neutral", neutral.tone());
    }
}
