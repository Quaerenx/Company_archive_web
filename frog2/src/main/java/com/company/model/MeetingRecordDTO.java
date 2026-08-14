package com.company.model;

import java.sql.Timestamp;
import java.util.Locale;

public class MeetingRecordDTO {
    private Long meetingId;
    private String title;
    private Timestamp meetingDatetime;
    private String meetingType;
    private String content;
    private String authorId;
    private String authorName;
    private Integer viewCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 추가 필드 (댓글 개수 등)
    private Integer commentCount;

    // 기본 생성자
    public MeetingRecordDTO() {}

    // 게터와 세터 메소드
    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Timestamp getMeetingDatetime() {
        return meetingDatetime;
    }

    public void setMeetingDatetime(Timestamp meetingDatetime) {
        this.meetingDatetime = meetingDatetime;
    }

    public String getMeetingType() {
        return meetingType;
    }

    public void setMeetingType(String meetingType) {
        this.meetingType = meetingType;
    }

    public String getMeetingTypeLabel() {
        if (meetingType == null || meetingType.isBlank()) {
            return "기타";
        }
        String normalizedType = meetingType.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedType) {
            case "daily" -> "일일 회의";
            case "weekly" -> "주간 회의";
            case "monthly" -> "월간 회의";
            case "project" -> "프로젝트 회의";
            case "emergency" -> "긴급 회의";
            case "other" -> "기타";
            default -> meetingType.trim();
        };
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }
}
