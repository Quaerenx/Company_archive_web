package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DomainServletValidationTest {
    @Test
    void missingMeetingUpdateAndDeleteIdsReturnJsonBadRequestInsteadOfEmptySuccess()
            throws Exception {
        for (String action : new String[] {"update", "delete"}) {
            RequestFixture request = new RequestFixture("/frog2/meeting");
            request.parameters.put("action", action);
            ResponseFixture response = new ResponseFixture();

            new MeetingServlet().doPost(request.proxy(), response.proxy());

            assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status, action);
            assertTrue(response.body.toString().contains(
                    "\"code\":\"invalid_meeting_request\""), action);
        }
    }

    @Test
    void invalidTroubleshootingAllowlistValueIsRejectedBeforeDaoUse() throws Exception {
        RequestFixture request = new RequestFixture("/frog2/troubleshooting");
        request.parameters.put("action", "add");
        request.parameters.put("title", "Issue");
        request.parameters.put("customer_name", "Acme");
        request.parameters.put("support_type", "arbitrary");
        ResponseFixture response = new ResponseFixture();

        new TroubleshootingServlet().doPost(request.proxy(), response.proxy());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertTrue(response.body.toString().contains(
                "\"code\":\"invalid_troubleshooting_request\""));
    }

    @Test
    void meetingEditAuthorizationReusesTheAlreadyLoadedRecord() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/company/controller/MeetingServlet.java"));
        int editStart = source.indexOf("} else if (\"edit\".equals(viewType))");
        int postStart = source.indexOf("protected void doPost", editStart);
        String editBranch = source.substring(editStart, postStart);

        assertTrue(editBranch.contains(
                "Objects.equals(meeting.getAuthorId(), user.getUserId())"));
        assertFalse(editBranch.contains("meetingDAO.isAuthor("));
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final HttpSession session = session();
        private final String requestUri;

        private RequestFixture(String requestUri) {
            this.requestUri = requestUri;
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getSession" -> session;
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "getContextPath" -> "/frog2";
                        case "getRequestURI" -> requestUri;
                        case "getMethod" -> "POST";
                        case "getHeader" -> "Accept".equals(args[0])
                                ? "application/json"
                                : null;
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private static HttpSession session() {
            UserDTO user = new UserDTO("user-1", "", "Tester", "QA");
            return (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[] {HttpSession.class},
                    (ignored, call, args) -> {
                        if ("getAttribute".equals(call.getName()) && "user".equals(args[0])) {
                            return user;
                        }
                        return defaultValue(call.getReturnType());
                    });
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

}
