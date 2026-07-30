package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.model.MeetingRecordDTO;
import com.company.model.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MeetingRequestMapperTest {
    private final MeetingRequestMapper mapper = new MeetingRequestMapper();

    @Test
    void mapsAndTrimsAllowedMeetingFormValues() {
        Map<String, String> parameters = validParameters();
        parameters.put("title", "  Weekly review  ");
        parameters.put("content", "  Decisions and actions  ");

        MeetingRecordDTO meeting = mapper.mapCreate(
                request(parameters), new UserDTO("user-1", "", "Tester", "QA"));

        assertEquals("Weekly review", meeting.getTitle());
        assertEquals("weekly", meeting.getMeetingType());
        assertEquals(Timestamp.valueOf("2026-07-30 13:45:00"), meeting.getMeetingDatetime());
        assertEquals("Decisions and actions", meeting.getContent());
        assertEquals("user-1", meeting.getAuthorId());
        assertEquals("Tester", meeting.getAuthorName());
    }

    @Test
    void rejectsMissingIdInvalidDateAndValuesOutsideTheFormAllowlist() {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.mapUpdate(request(validParameters())));

        Map<String, String> invalidDate = validParameters();
        invalidDate.put("meeting_datetime", "2026-02-30T10:00");
        assertThrows(IllegalArgumentException.class,
                () -> mapper.mapCreate(request(invalidDate), user()));

        Map<String, String> invalidType = validParameters();
        invalidType.put("meeting_type", "arbitrary");
        assertThrows(IllegalArgumentException.class,
                () -> mapper.mapCreate(request(invalidType), user()));

        Map<String, String> blankContent = validParameters();
        blankContent.put("content", "  ");
        assertThrows(IllegalArgumentException.class,
                () -> mapper.mapCreate(request(blankContent), user()));
    }

    @Test
    void normalizesPageBoundariesWithoutOffsetOverflow() {
        assertEquals(1, mapper.requestedPage(request(Map.of())));
        assertEquals(1, mapper.requestedPage(request(Map.of("page", "-1"))));
        assertEquals(Integer.MAX_VALUE,
                mapper.requestedPage(request(Map.of("page", "999999999999"))));

        assertEquals(0, MeetingRequestMapper.totalPages(0, 20));
        assertEquals(3, MeetingRequestMapper.totalPages(41, 20));
        assertEquals(107_374_183,
                MeetingRequestMapper.totalPages(Integer.MAX_VALUE, 20));
        assertEquals(1, MeetingRequestMapper.clampPage(Integer.MAX_VALUE, 0));
        assertEquals(3, MeetingRequestMapper.clampPage(Integer.MAX_VALUE, 3));
    }

    private static Map<String, String> validParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("title", "Weekly review");
        parameters.put("meeting_type", "weekly");
        parameters.put("meeting_datetime", "2026-07-30T13:45");
        parameters.put("content", "Decisions and actions");
        return parameters;
    }

    private static UserDTO user() {
        return new UserDTO("user-1", "", "Tester", "QA");
    }

    private static HttpServletRequest request(Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, call, args) -> {
                    if ("getParameter".equals(call.getName())) {
                        return parameters.get((String) args[0]);
                    }
                    return defaultValue(call.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
