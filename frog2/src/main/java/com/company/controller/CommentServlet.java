package com.company.controller;

import com.company.model.MeetingCommentDAO;
import com.company.model.MeetingCommentDTO;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import com.company.web.JsonResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CommentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserDTO user = SessionPrincipal.from(request);
        if (user == null) {
            JsonResponse.sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "authentication_required",
                    "로그인이 필요합니다.");
            return;
        }

        String action = request.getParameter("action");
        MeetingCommentDAO commentDAO = new MeetingCommentDAO();
        if ("add".equals(action)) {
            handleAddComment(request, response, user, commentDAO);
        } else if ("update".equals(action)) {
            handleUpdateComment(request, response, user, commentDAO);
        } else if ("delete".equals(action)) {
            handleDeleteComment(request, response, user, commentDAO);
        } else {
            JsonResponse.sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_action",
                    "잘못된 요청입니다.");
        }
    }

    private void handleAddComment(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user,
            MeetingCommentDAO commentDAO) throws IOException {
        String meetingIdValue = request.getParameter("meeting_id");
        String content = request.getParameter("content");
        if (meetingIdValue == null || meetingIdValue.isBlank()) {
            JsonResponse.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "meeting_id_required", "회의록 ID가 필요합니다.");
            return;
        }
        if (content == null || content.isBlank()) {
            JsonResponse.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "comment_required", "댓글 내용을 입력해주세요.");
            return;
        }

        long meetingId;
        try {
            meetingId = Long.parseLong(meetingIdValue);
        } catch (NumberFormatException exception) {
            JsonResponse.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_meeting_id", "잘못된 회의록 ID입니다.");
            return;
        }

        MeetingCommentDTO comment = new MeetingCommentDTO();
        comment.setMeetingId(meetingId);
        comment.setContent(content.trim());
        comment.setAuthorId(user.getUserId());
        comment.setAuthorName(user.getUserName());
        if (commentDAO.addComment(comment)) {
            JsonResponse.sendSuccess(
                    response, HttpServletResponse.SC_OK, "댓글이 성공적으로 등록되었습니다.");
        } else {
            JsonResponse.sendError(response, HttpServletResponse.SC_CONFLICT,
                    "comment_not_created", "댓글을 등록하지 못했습니다.");
        }
    }

    private void handleUpdateComment(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user,
            MeetingCommentDAO commentDAO) throws IOException {
        String commentIdValue = request.getParameter("comment_id");
        String content = request.getParameter("content");
        if (commentIdValue == null || commentIdValue.isBlank()) {
            JsonResponse.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "comment_id_required", "댓글 ID가 필요합니다.");
            return;
        }
        if (content == null || content.isBlank()) {
            JsonResponse.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "comment_required", "댓글 내용을 입력해주세요.");
            return;
        }

        Long commentId = parseCommentId(commentIdValue, response);
        if (commentId == null) {
            return;
        }

        MeetingCommentDTO comment = new MeetingCommentDTO();
        comment.setCommentId(commentId);
        comment.setContent(content.trim());
        if (commentDAO.updateCommentForAuthor(
                comment, user.getUserId())) {
            JsonResponse.sendSuccess(
                    response, HttpServletResponse.SC_OK, "댓글이 성공적으로 수정되었습니다.");
        } else {
            JsonResponse.sendError(response, HttpServletResponse.SC_FORBIDDEN,
                    "comment_forbidden",
                    "댓글 수정 권한이 없거나 댓글이 존재하지 않습니다.");
        }
    }

    private void handleDeleteComment(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user,
            MeetingCommentDAO commentDAO) throws IOException {
        String commentIdValue = request.getParameter("comment_id");
        if (commentIdValue == null || commentIdValue.isBlank()) {
            JsonResponse.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "comment_id_required", "댓글 ID가 필요합니다.");
            return;
        }

        Long commentId = parseCommentId(commentIdValue, response);
        if (commentId == null) {
            return;
        }

        if (commentDAO.deleteCommentForAuthor(
                commentId, user.getUserId())) {
            JsonResponse.sendSuccess(
                    response, HttpServletResponse.SC_OK, "댓글이 성공적으로 삭제되었습니다.");
        } else {
            JsonResponse.sendError(response, HttpServletResponse.SC_FORBIDDEN,
                    "comment_forbidden",
                    "댓글 삭제 권한이 없거나 댓글이 존재하지 않습니다.");
        }
    }

    private static Long parseCommentId(String value, HttpServletResponse response) throws IOException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            JsonResponse.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_comment_id", "잘못된 댓글 ID입니다.");
            return null;
        }
    }
}
