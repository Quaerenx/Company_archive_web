package com.company.model;

import java.sql.Date;

public record MeetingListFilter(
        String query,
        String meetingType,
        String author,
        Date startDate,
        Date endDate) {
    public static MeetingListFilter empty() {
        return new MeetingListFilter(null, null, null, null, null);
    }

    public boolean isActive() {
        return query != null || meetingType != null || author != null
                || startDate != null || endDate != null;
    }
}
