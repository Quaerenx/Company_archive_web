package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.MeetingCommentDAO;
import com.company.model.MeetingCommentDTO;
import com.company.model.MeetingCommentPage;
import com.company.model.MeetingRecordDAO;
import com.company.model.MeetingRecordDTO;
import com.company.model.UserDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MeetingServletDependencyTest {
    @Test
    void listUsesTheInjectedMeetingDao() throws Exception {
        StubMeetingRecordDAO meetingDAO = new StubMeetingRecordDAO();
        MeetingRecordDTO meeting = meeting(7L);
        meetingDAO.totalCount = 1;
        meetingDAO.records = List.of(meeting);
        MeetingServlet servlet = new MeetingServlet(
                meetingDAO, new StubMeetingCommentDAO());
        RequestFixture request = new RequestFixture();
        request.parameters.put("view", "list");

        servlet.doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals(1, meetingDAO.countCalls);
        assertEquals(1, meetingDAO.listCalls);
        assertSame(meetingDAO.records,
                request.attributes.get("meetingList"));
        assertEquals("/meeting/meeting_list.jsp", request.forwardedPath);
    }

    @Test
    void detailUsesBothInjectedDaos() throws Exception {
        StubMeetingRecordDAO meetingDAO = new StubMeetingRecordDAO();
        meetingDAO.detail = meeting(7L);
        StubMeetingCommentDAO commentDAO = new StubMeetingCommentDAO();
        MeetingCommentDTO comment = new MeetingCommentDTO();
        comment.setCommentId(11L);
        commentDAO.page = new MeetingCommentPage(
                List.of(comment), null, false, MeetingCommentDAO.PAGE_SIZE);
        MeetingServlet servlet = new MeetingServlet(meetingDAO, commentDAO);
        RequestFixture request = new RequestFixture();
        request.parameters.put("view", "view");
        request.parameters.put("id", "7");

        servlet.doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals(7L, meetingDAO.lastDetailId);
        assertEquals(7L, commentDAO.lastMeetingId);
        assertSame(meetingDAO.detail, request.attributes.get("meeting"));
        assertSame(commentDAO.page, request.attributes.get("commentPage"));
        assertEquals("/meeting/meeting_view.jsp", request.forwardedPath);
    }

    @Test
    void commentCreateUsesTheInjectedCommentDao() throws Exception {
        StubMeetingCommentDAO commentDAO = new StubMeetingCommentDAO();
        commentDAO.createSucceeds = true;
        CommentServlet servlet = new CommentServlet(commentDAO);
        RequestFixture request = new RequestFixture();
        request.parameters.put("action", "add");
        request.parameters.put("meeting_id", "7");
        request.parameters.put("content", " Follow-up ");
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertEquals(7L, commentDAO.created.getMeetingId());
        assertEquals("Follow-up", commentDAO.created.getContent());
        assertEquals("user-1", commentDAO.created.getAuthorId());
        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertTrue(response.body.toString().contains("\"success\":true"));
    }

    private static MeetingRecordDTO meeting(long id) {
        MeetingRecordDTO meeting = new MeetingRecordDTO();
        meeting.setMeetingId(id);
        meeting.setTitle("Weekly meeting");
        meeting.setAuthorId("user-1");
        return meeting;
    }

    private static final class StubMeetingRecordDAO
            extends MeetingRecordDAO {
        private int totalCount;
        private List<MeetingRecordDTO> records = List.of();
        private MeetingRecordDTO detail;
        private int countCalls;
        private int listCalls;
        private long lastDetailId;

        @Override
        public int getTotalCount() {
            countCalls++;
            return totalCount;
        }

        @Override
        public List<MeetingRecordDTO> getMeetingRecords(int page) {
            listCalls++;
            return records;
        }

        @Override
        public MeetingRecordDTO getMeetingRecord(Long meetingId) {
            lastDetailId = meetingId;
            return detail;
        }
    }

    private static final class StubMeetingCommentDAO
            extends MeetingCommentDAO {
        private MeetingCommentPage page = new MeetingCommentPage(
                List.of(), null, false, PAGE_SIZE);
        private long lastMeetingId;
        private MeetingCommentDTO created;
        private boolean createSucceeds;

        @Override
        public MeetingCommentPage getCommentPage(
                Long meetingId, Long beforeCommentId, int pageSize) {
            lastMeetingId = meetingId;
            return page;
        }

        @Override
        public boolean addComment(MeetingCommentDTO comment) {
            created = comment;
            return createSucceeds;
        }
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpSession session = session();
        private String forwardedPath;

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getSession" -> session;
                        case "getParameter" ->
                                parameters.get((String) args[0]);
                        case "getContextPath" -> "/frog2";
                        case "getRequestURI" -> "/frog2/meeting";
                        case "getMethod" -> "GET";
                        case "getAttribute" ->
                                attributes.get((String) args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getRequestDispatcher" ->
                                dispatcher((String) args[0]);
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private RequestDispatcher dispatcher(String path) {
            return (RequestDispatcher) Proxy.newProxyInstance(
                    RequestDispatcher.class.getClassLoader(),
                    new Class<?>[] {RequestDispatcher.class},
                    (ignored, call, args) -> {
                        if ("forward".equals(call.getName())) {
                            forwardedPath = path;
                        }
                        return defaultValue(call.getReturnType());
                    });
        }

        private static HttpSession session() {
            UserDTO user = new UserDTO(
                    "user-1", "", "Tester", "QA");
            return (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[] {HttpSession.class},
                    (ignored, call, args) ->
                            "getAttribute".equals(call.getName())
                                    && "user".equals(args[0])
                                    ? user
                                    : defaultValue(call.getReturnType()));
        }
    }

    private static final class ResponseFixture {
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private int status = HttpServletResponse.SC_OK;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "isCommitted" -> false;
                        case "resetBuffer" -> {
                            body.getBuffer().setLength(0);
                            yield null;
                        }
                        case "setStatus" -> {
                            status = (Integer) args[0];
                            yield null;
                        }
                        case "getWriter" -> writer;
                        default -> defaultValue(call.getReturnType());
                    });
        }
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
        if (type == long.class) {
            return 0L;
        }
        return 0;
    }
}
