package com.company.controller;

import com.company.model.MeetingRecordDTO;
import com.company.model.UserDTO;
import com.company.util.StrictDateParser;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.Set;

final class MeetingRequestMapper {
    private static final int MAX_TITLE_LENGTH = 200;
    private static final Set<String> MEETING_TYPES = Set.of(
            "daily", "weekly", "monthly", "project", "emergency", "other");

    MeetingRecordDTO mapCreate(HttpServletRequest request, UserDTO user) {
        MeetingRecordDTO meeting = mapForm(request);
        meeting.setAuthorId(user.getUserId());
        meeting.setAuthorName(user.getUserName());
        return meeting;
    }

    MeetingRecordDTO mapUpdate(HttpServletRequest request) {
        long meetingId = positiveLong(request, "meeting_id");
        MeetingRecordDTO meeting = mapForm(request);
        meeting.setMeetingId(meetingId);
        return meeting;
    }

    long positiveLong(HttpServletRequest request, String parameterName) {
        String value = trimmed(request.getParameter(parameterName));
        if (value == null) {
            throw invalid("회의록 ID가 필요합니다.");
        }
        try {
            long id = Long.parseLong(value);
            if (id <= 0) {
                throw invalid("회의록 ID가 올바르지 않습니다.");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw invalid("회의록 ID가 올바르지 않습니다.");
        }
    }

    int requestedPage(HttpServletRequest request) {
        String value = trimmed(request.getParameter("page"));
        if (value == null) {
            return 1;
        }
        try {
            long page = Long.parseLong(value);
            if (page < 1) {
                return 1;
            }
            return page > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) page;
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    static int totalPages(int totalCount, int pageSize) {
        if (totalCount < 0 || pageSize <= 0) {
            throw new IllegalArgumentException("Pagination values must be positive.");
        }
        return totalCount == 0 ? 0 : ((totalCount - 1) / pageSize) + 1;
    }

    static int clampPage(int requestedPage, int totalPages) {
        if (totalPages <= 0) {
            return 1;
        }
        return Math.min(Math.max(requestedPage, 1), totalPages);
    }

    private MeetingRecordDTO mapForm(HttpServletRequest request) {
        String title = required(request, "title", "회의 제목을 입력해주세요.");
        if (title.length() > MAX_TITLE_LENGTH) {
            throw invalid("회의 제목은 200자 이하로 입력해주세요.");
        }

        String meetingType = required(request, "meeting_type", "회의 유형을 선택해주세요.");
        if (!MEETING_TYPES.contains(meetingType)) {
            throw invalid("회의 유형이 올바르지 않습니다.");
        }

        String dateTimeValue = required(
                request, "meeting_datetime", "회의 일시를 선택해주세요.");
        Timestamp meetingDatetime = StrictDateParser.parseTimestampOrNull(dateTimeValue);
        if (meetingDatetime == null) {
            throw invalid("회의 일시가 올바르지 않습니다.");
        }

        MeetingRecordDTO meeting = new MeetingRecordDTO();
        meeting.setTitle(title);
        meeting.setMeetingDatetime(meetingDatetime);
        meeting.setMeetingType(meetingType);
        meeting.setContent(required(request, "content", "회의 내용을 입력해주세요."));
        return meeting;
    }

    private static String required(
            HttpServletRequest request, String parameterName, String message) {
        String value = trimmed(request.getParameter(parameterName));
        if (value == null) {
            throw invalid(message);
        }
        return value;
    }

    private static String trimmed(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}
