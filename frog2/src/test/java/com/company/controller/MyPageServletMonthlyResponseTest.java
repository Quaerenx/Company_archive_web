package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.MonthlyCustomerResponseDAO;
import com.company.model.MonthlyCustomerResponseDTO;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MyPageServletMonthlyResponseTest {
    @Test
    void addAndUpdateRejectMissingRequiredValuesWithFlashError()
            throws Exception {
        for (String action : new String[] {"addResponse", "updateResponse"}) {
            for (String missing : new String[] {
                    "responseDate", "customerName", "reason"}) {
                RequestFixture request = new RequestFixture();
                request.parameters.put("formAction", action);
                request.parameters.put("responseId", "7");
                request.parameters.put("responseDate", "2026-08-30");
                request.parameters.put("customerName", "Acme");
                request.parameters.put("reason", "Support");
                request.parameters.put("year", "2026");
                request.parameters.put("month", "8");
                request.parameters.put(missing, "  ");
                ResponseFixture response = new ResponseFixture();

                new MyPageServlet().doPost(
                        request.proxy(), response.proxy());

                assertTrue(response.redirect.startsWith(
                        "/frog2/mypage?action=monthlyResponse"
                                + "&year=2026&month=8&_flash="));
                String token = URI.create(response.redirect)
                        .getQuery()
                        .replaceFirst(".*(?:^|&)_flash=", "");
                request.parameters.put(FlashMessage.PARAMETER_NAME, token);
                FlashMessage.expose(request.proxy());
                assertEquals(
                        "날짜, 고객명, 사유를 모두 입력해 주세요.",
                        request.attributes.get("message"));
                assertEquals("error",
                        request.attributes.get("messageType"));
            }
        }
    }

    @Test
    void routesValidMonthlyWritesThroughTheInjectedCommandService()
            throws Exception {
        StubMonthlyResponseDAO dao = new StubMonthlyResponseDAO();
        dao.writeResult = true;
        MyPageServlet servlet = new MyPageServlet(
                new UserDAO(), dao, new MyPageRequestMapper());

        RequestFixture add = validMonthlyRequest("addResponse");
        ResponseFixture addResponse = new ResponseFixture();
        servlet.doPost(add.proxy(), addResponse.proxy());
        assertEquals("user-1", dao.added.getUserId());
        assertEquals("Acme", dao.added.getCustomerName());
        assertTrue(addResponse.redirect.startsWith(
                "/frog2/mypage?action=monthlyResponse"
                        + "&year=2026&month=8&_flash="));

        RequestFixture update = validMonthlyRequest("updateResponse");
        update.parameters.put("responseId", "17");
        ResponseFixture updateResponse = new ResponseFixture();
        servlet.doPost(update.proxy(), updateResponse.proxy());
        assertEquals(17, dao.updated.getId());
        assertEquals("user-1", dao.updated.getUserId());

        RequestFixture delete = validMonthlyRequest("deleteResponse");
        delete.parameters.put("responseId", "17");
        ResponseFixture deleteResponse = new ResponseFixture();
        servlet.doPost(delete.proxy(), deleteResponse.proxy());
        assertEquals(17, dao.deletedId);
        assertEquals("user-1", dao.deletedOwner);
    }

    private static RequestFixture validMonthlyRequest(String action) {
        RequestFixture request = new RequestFixture();
        request.parameters.put("formAction", action);
        request.parameters.put("responseDate", "2026-08-30");
        request.parameters.put("customerName", "Acme");
        request.parameters.put("reason", "Support");
        request.parameters.put("actionContent", "Resolved");
        request.parameters.put("year", "2026");
        request.parameters.put("month", "8");
        return request;
    }

    private static final class StubMonthlyResponseDAO
            extends MonthlyCustomerResponseDAO {
        private boolean writeResult;
        private MonthlyCustomerResponseDTO added;
        private MonthlyCustomerResponseDTO updated;
        private int deletedId;
        private String deletedOwner;

        @Override
        public boolean addResponse(MonthlyCustomerResponseDTO response) {
            added = response;
            return writeResult;
        }

        @Override
        public boolean updateResponse(MonthlyCustomerResponseDTO response) {
            updated = response;
            return writeResult;
        }

        @Override
        public boolean deleteResponse(int responseId, String userId) {
            deletedId = responseId;
            deletedOwner = userId;
            return writeResult;
        }
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, Object> sessionAttributes = new HashMap<>();
        private final HttpSession session;

        private RequestFixture() {
            UserDTO user = new UserDTO("user-1", "", "User", "QA");
            sessionAttributes.put(SessionPrincipal.SESSION_ATTRIBUTE, user);
            session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[] {HttpSession.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getAttribute" ->
                                sessionAttributes.get((String) args[0]);
                        case "setAttribute" -> {
                            sessionAttributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "removeAttribute" -> {
                            sessionAttributes.remove((String) args[0]);
                            yield null;
                        }
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getSession" -> session;
                        case "getParameter" ->
                                parameters.get((String) args[0]);
                        case "getContextPath" -> "/frog2";
                        case "getAttribute" ->
                                attributes.get((String) args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static final class ResponseFixture {
        private String redirect;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> {
                        if ("sendRedirect".equals(call.getName())) {
                            redirect = (String) args[0];
                        }
                        return defaultValue(call.getReturnType());
                    });
        }
    }

}
