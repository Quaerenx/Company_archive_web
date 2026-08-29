package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
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

    @Test
    void exposesJavaBeanPropertiesForJspExpressionLanguage() throws Exception {
        PropertyDescriptor[] properties = Introspector
                .getBeanInfo(CustomerEosNotice.class)
                .getPropertyDescriptors();

        assertNotNull(readMethod(properties, "daysRemaining"));
        assertNotNull(readMethod(properties, "tone"));
        assertNotNull(readMethod(properties, "message"));
    }

    private static Object readMethod(PropertyDescriptor[] properties, String name) {
        return Arrays.stream(properties)
                .filter(property -> name.equals(property.getName()))
                .map(PropertyDescriptor::getReadMethod)
                .findFirst()
                .orElse(null);
    }
}
