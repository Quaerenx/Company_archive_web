package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MeetingRecordDTOTest {
    @Test
    void meetingTypeCodesExposeStableKoreanLabels() {
        MeetingRecordDTO meeting = new MeetingRecordDTO();

        meeting.setMeetingType("daily");
        assertEquals("일일 회의", meeting.getMeetingTypeLabel());

        meeting.setMeetingType("PROJECT");
        assertEquals("프로젝트 회의", meeting.getMeetingTypeLabel());

        meeting.setMeetingType(null);
        assertEquals("기타", meeting.getMeetingTypeLabel());

        meeting.setMeetingType("legacy-label");
        assertEquals("legacy-label", meeting.getMeetingTypeLabel());
    }
}
