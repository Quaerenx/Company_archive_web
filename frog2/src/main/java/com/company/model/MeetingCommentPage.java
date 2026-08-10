package com.company.model;

import java.util.List;

public final class MeetingCommentPage {
    private final List<MeetingCommentDTO> comments;
    private final Long nextBeforeCommentId;
    private final boolean hasOlder;
    private final int pageSize;

    public MeetingCommentPage(
            List<MeetingCommentDTO> comments,
            Long nextBeforeCommentId,
            boolean hasOlder,
            int pageSize) {
        this.comments = List.copyOf(comments);
        this.nextBeforeCommentId = nextBeforeCommentId;
        this.hasOlder = hasOlder;
        this.pageSize = pageSize;
    }

    public List<MeetingCommentDTO> getComments() {
        return comments;
    }

    public Long getNextBeforeCommentId() {
        return nextBeforeCommentId;
    }

    public boolean isHasOlder() {
        return hasOlder;
    }

    public int getPageSize() {
        return pageSize;
    }
}
