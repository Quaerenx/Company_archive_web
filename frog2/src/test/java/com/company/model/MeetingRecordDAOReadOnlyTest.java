package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.config.ApplicationEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MeetingRecordDAOReadOnlyTest {
    @AfterEach
    void clearProperties() {
        System.clearProperty(ApplicationEnvironment.ENV_PROPERTY);
        System.clearProperty(ApplicationEnvironment.READ_ONLY_PROPERTY);
    }

    @Test
    void viewCountIncrementDoesNotInitializeDatabaseInDevelopment() {
        System.setProperty(ApplicationEnvironment.ENV_PROPERTY, "dev");
        System.setProperty(ApplicationEnvironment.READ_ONLY_PROPERTY, "false");

        assertFalse(new MeetingRecordDAO().incrementViewCount(1L));
    }

    @Test
    void pageOffsetRejectsInvalidAndOverflowingValuesBeforeJdbcUse() {
        assertEquals(0, MeetingRecordDAO.offsetForPage(1));
        assertEquals(40, MeetingRecordDAO.offsetForPage(3));
        assertThrows(IllegalArgumentException.class,
                () -> MeetingRecordDAO.offsetForPage(0));
        assertThrows(ArithmeticException.class,
                () -> MeetingRecordDAO.offsetForPage(Integer.MAX_VALUE));
    }
}
