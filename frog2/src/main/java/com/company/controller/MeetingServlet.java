package com.company.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.company.model.MeetingCommentDAO;
import com.company.model.MeetingCommentDTO;
import com.company.model.MeetingCommentPage;
import com.company.model.MeetingRecordDAO;
import com.company.model.MeetingRecordDTO;
import com.company.model.PageResult;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import com.company.web.ApplicationError;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// @WebServlet("/meeting") - web.xml에서 매핑하므로 주석 처리
public class MeetingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final MeetingRequestMapper requestMapper = new MeetingRequestMapper();
    private final MeetingRecordDAO meetingDAO;
    private final MeetingCommentDAO commentDAO;

    public MeetingServlet() {
        this(new MeetingRecordDAO(), new MeetingCommentDAO());
    }

    MeetingServlet(
            MeetingRecordDAO meetingDAO,
            MeetingCommentDAO commentDAO) {
        this.meetingDAO = Objects.requireNonNull(
                meetingDAO, "meetingDAO");
        this.commentDAO = Objects.requireNonNull(
                commentDAO, "commentDAO");
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 세션 확인
        HttpSession session = request.getSession(false);
        UserDTO user = SessionPrincipal.expose(request, session);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // 뷰 타입 확인
        String viewType = request.getParameter("view");
        if (viewType == null || viewType.isEmpty()) {
            viewType = "list";
        }

        if ("list".equals(viewType)) {
            // 회의록 목록 (페이징 처리)
            int requestedPage = requestMapper.requestedPage(request);
            PageResult<MeetingRecordDTO> meetingPage =
                    meetingDAO.getMeetingPage(requestedPage);

            request.setAttribute("meetingList", meetingPage.items());
            request.setAttribute("currentPage", meetingPage.page());
            request.setAttribute("totalPages", meetingPage.totalPages());
            request.setAttribute("totalCount", meetingPage.totalCount());
            request.setAttribute("viewType", "list");
            request.getRequestDispatcher("/meeting/meeting_list.jsp").forward(request, response);

        } else if ("view".equals(viewType)) {
            // 회의록 상세 조회
            long meetingId;
            try {
                meetingId = requestMapper.positiveLong(request, "id");
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            MeetingRecordDTO meeting = meetingDAO.getMeetingRecord(meetingId);

            if (meeting != null) {
                // 댓글 목록 조회
                Long commentBefore;
                try {
                    commentBefore = requestMapper.optionalPositiveLong(
                            request, "commentBefore");
                } catch (IllegalArgumentException exception) {
                    sendBadRequest(request, response, exception);
                    return;
                }
                MeetingCommentPage commentPage = commentDAO.getCommentPage(
                        meetingId, commentBefore, MeetingCommentDAO.PAGE_SIZE);
                List<MeetingCommentDTO> comments = commentPage.getComments();

                request.setAttribute("meeting", meeting);
                request.setAttribute("comments", comments);
                request.setAttribute("commentPage", commentPage);
                request.setAttribute("viewType", "view");
                request.getRequestDispatcher("/meeting/meeting_view.jsp").forward(request, response);
            } else {
                session.setAttribute("error", "존재하지 않는 회의록입니다.");
                response.sendRedirect("meeting?view=list");
            }

        } else if ("write".equals(viewType)) {
            // 회의록 작성 폼
            request.setAttribute("viewType", "write");
            request.getRequestDispatcher("/meeting/meeting_write.jsp").forward(request, response);

        } else if ("edit".equals(viewType)) {
            // 회의록 수정 폼
            long meetingId;
            try {
                meetingId = requestMapper.positiveLong(request, "id");
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            MeetingRecordDTO meeting = meetingDAO.getMeetingRecord(meetingId);
            if (meeting != null) {
                // 상세 조회 결과에 포함된 작성자 ID를 재사용해 추가 SELECT를 피한다.
                if (Objects.equals(meeting.getAuthorId(), user.getUserId())) {
                    request.setAttribute("meeting", meeting);
                    request.setAttribute("viewType", "edit");
                    request.getRequestDispatcher("/meeting/meeting_edit.jsp").forward(request, response);
                } else {
                    session.setAttribute("error", "수정 권한이 없습니다.");
                    response.sendRedirect("meeting?view=view&id=" + meetingId);
                }
            } else {
                session.setAttribute("error", "존재하지 않는 회의록입니다.");
                response.sendRedirect("meeting?view=list");
            }

        } else {
            response.sendRedirect("meeting?view=list");
        }
    }

    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 세션 확인
        HttpSession session = request.getSession(false);
        UserDTO user = SessionPrincipal.from(session);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String actionType = request.getParameter("action");

        if ("write".equals(actionType)) {
            MeetingRecordDTO meeting;
            try {
                meeting = requestMapper.mapCreate(request, user);
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            boolean success = meetingDAO.addMeetingRecord(meeting);
            if (success) {
                session.setAttribute("message", "회의록이 성공적으로 등록되었습니다.");
            } else {
                session.setAttribute("error", "회의록 등록 중 오류가 발생했습니다.");
            }
            response.sendRedirect("meeting?view=list");

        } else if ("update".equals(actionType)) {
            MeetingRecordDTO meeting;
            try {
                meeting = requestMapper.mapUpdate(request);
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            long meetingId = meeting.getMeetingId();
            boolean success = meetingDAO.updateMeetingRecordForAuthor(
                    meeting, user.getUserId());
            if (success) {
                session.setAttribute("message", "회의록이 성공적으로 수정되었습니다.");
                response.sendRedirect("meeting?view=view&id=" + meetingId);
            } else {
                session.setAttribute(
                        "error",
                        "수정 권한이 없거나 회의록이 존재하지 않습니다.");
                response.sendRedirect("meeting?view=list");
            }

        } else if ("delete".equals(actionType)) {
            long meetingId;
            try {
                meetingId = requestMapper.positiveLong(request, "meeting_id");
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            boolean success = meetingDAO.deleteMeetingRecordForAuthor(
                    meetingId, user.getUserId());
            if (success) {
                session.setAttribute("message", "회의록이 성공적으로 삭제되었습니다.");
            } else {
                session.setAttribute(
                        "error",
                        "삭제 권한이 없거나 회의록이 존재하지 않습니다.");
            }
            response.sendRedirect("meeting?view=list");

        } else {
            response.sendRedirect("meeting?view=list");
        }
    }

    private static void sendBadRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            IllegalArgumentException exception) throws IOException {
        ApplicationError.send(
                request,
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "invalid_meeting_request",
                exception.getMessage());
    }
}
