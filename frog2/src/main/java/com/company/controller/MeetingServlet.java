package com.company.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import com.company.model.MeetingCommentDAO;
import com.company.model.MeetingCommentDTO;
import com.company.model.MeetingCommentPage;
import com.company.model.MeetingListFilter;
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
        FlashMessage.expose(request);

        // 뷰 타입 확인
        String viewType = request.getParameter("view");
        if (viewType == null || viewType.isEmpty()) {
            viewType = "list";
        }

        if ("list".equals(viewType)) {
            // 회의록 목록 (페이징 처리)
            int requestedPage = requestMapper.requestedPage(request);
            MeetingListFilter filter;
            try {
                filter = requestMapper.listFilter(request);
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }
            PageResult<MeetingRecordDTO> meetingPage =
                    filter.isActive()
                            ? meetingDAO.getMeetingPage(filter, requestedPage)
                            : meetingDAO.getMeetingPage(requestedPage);

            request.setAttribute("meetingList", meetingPage.items());
            request.setAttribute("currentPage", meetingPage.page());
            request.setAttribute("totalPages", meetingPage.totalPages());
            request.setAttribute("totalCount", meetingPage.totalCount());
            request.setAttribute("q", filter.query());
            request.setAttribute("meetingType", filter.meetingType());
            request.setAttribute("author", filter.author());
            request.setAttribute("startDate", filter.startDate());
            request.setAttribute("endDate", filter.endDate());
            request.setAttribute("meetingFilterActive", filter.isActive());
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
                FlashMessage.redirect(
                        request,
                        response,
                        listReturnPath(request),
                        "존재하지 않는 회의록입니다.",
                        "error");
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
                    FlashMessage.redirect(
                            request,
                            response,
                            "meeting?view=view&id=" + meetingId
                                    + detailReturnSuffix(request),
                            "수정 권한이 없습니다.",
                            "error");
                }
            } else {
                FlashMessage.redirect(
                        request,
                        response,
                        listReturnPath(request),
                        "존재하지 않는 회의록입니다.",
                        "error");
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
            FlashMessage.redirect(
                    request,
                    response,
                    listReturnPath(request),
                    success
                            ? "회의록이 성공적으로 등록되었습니다."
                            : "회의록 등록 중 오류가 발생했습니다.",
                    success ? "success" : "error");

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
                FlashMessage.redirect(
                        request,
                        response,
                        "meeting?view=view&id=" + meetingId
                                + detailReturnSuffix(request),
                        "회의록이 성공적으로 수정되었습니다.",
                        "success");
            } else {
                FlashMessage.redirect(
                        request,
                        response,
                        listReturnPath(request),
                        "수정 권한이 없거나 회의록이 존재하지 않습니다.",
                        "error");
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
            FlashMessage.redirect(
                    request,
                    response,
                    listReturnPath(request),
                    success
                            ? "회의록이 성공적으로 삭제되었습니다."
                            : "삭제 권한이 없거나 회의록이 존재하지 않습니다.",
                    success ? "success" : "error");

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

    private static String detailReturnSuffix(HttpServletRequest request) {
        StringBuilder suffix = new StringBuilder();
        appendParameter(suffix, "returnPage", request.getParameter("returnPage"));
        appendParameter(suffix, "returnQ", request.getParameter("returnQ"));
        appendParameter(suffix, "returnType", request.getParameter("returnType"));
        appendParameter(suffix, "returnAuthor", request.getParameter("returnAuthor"));
        appendParameter(suffix, "returnStartDate", request.getParameter("returnStartDate"));
        appendParameter(suffix, "returnEndDate", request.getParameter("returnEndDate"));
        return suffix.toString();
    }

    private static String listReturnPath(HttpServletRequest request) {
        StringBuilder path = new StringBuilder("meeting?view=list");
        appendParameter(path, "page", request.getParameter("returnPage"));
        appendParameter(path, "q", request.getParameter("returnQ"));
        appendParameter(path, "type", request.getParameter("returnType"));
        appendParameter(path, "author", request.getParameter("returnAuthor"));
        appendParameter(path, "startDate", request.getParameter("returnStartDate"));
        appendParameter(path, "endDate", request.getParameter("returnEndDate"));
        return path.toString();
    }

    private static void appendParameter(
            StringBuilder target, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        target.append('&')
                .append(name)
                .append('=')
                .append(URLEncoder.encode(value.strip(), StandardCharsets.UTF_8));
    }
}
