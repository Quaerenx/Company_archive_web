package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MeetingRecordDAOReadOnlyTest {
    @Test
    void meetingGetPathContainsNoViewCountMutation() throws Exception {
        String servlet = Files.readString(Path.of(
                "src/main/java/com/company/controller/MeetingServlet.java"));
        String dao = Files.readString(Path.of(
                "src/main/java/com/company/model/MeetingRecordDAO.java"));

        assertFalse(servlet.contains("incrementViewCount"));
        assertFalse(dao.contains("view_count = view_count + 1"));
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
