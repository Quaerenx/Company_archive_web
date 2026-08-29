package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MyPageServletAccountPrgTest {
    @Test
    void profileValidationRejectsBlankAndOversizedNamesBeforeWriting()
            throws Exception {
        Map<String, String> invalidNames = Map.of(
                "   ", "이름을 입력해 주세요.",
                "가".repeat(101), "이름은 100자 이하로 입력해 주세요.");

        for (Map.Entry<String, String> invalid : invalidNames.entrySet()) {
            StubUserDAO userDAO = new StubUserDAO();
            RequestFixture request = new RequestFixture();
            request.parameters.put("formAction", "updateProfile");
            request.parameters.put("userName", invalid.getKey());
            ResponseFixture response = new ResponseFixture();

            new MyPageServlet(userDAO).doPost(
                    request.proxy(), response.proxy());

            assertEquals(0, userDAO.updateNameCalls);
            assertTrue(response.redirect.startsWith(
                    "/frog2/mypage?action=editProfile&_flash="));
            assertFlash(
                    request,
                    response,
                    invalid.getValue(),
                    "error");
        }
    }

    @Test
    void profileSuccessRedirectsToGetAndRefreshesSessionPrincipal()
            throws Exception {
        StubUserDAO userDAO = new StubUserDAO();
        userDAO.updateNameResult = true;
        RequestFixture request = new RequestFixture();
        request.parameters.put("formAction", "updateProfile");
        request.parameters.put("userName", "  새 이름  ");
        ResponseFixture response = new ResponseFixture();

        new MyPageServlet(userDAO).doPost(
                request.proxy(), response.proxy());

        assertEquals("새 이름", userDAO.updatedUserName);
        UserDTO stored = SessionPrincipal.from(request.session);
        assertEquals("새 이름", stored.getUserName());
        assertEquals("", stored.getPassword());
        assertTrue(response.redirect.startsWith("/frog2/mypage?_flash="));
        assertFlash(
                request,
                response,
                "프로필이 성공적으로 업데이트되었습니다.",
                "success");
    }

    @Test
    void profileWriteFailureRedirectsBackWithoutChangingSession()
            throws Exception {
        StubUserDAO userDAO = new StubUserDAO();
        RequestFixture request = new RequestFixture();
        request.parameters.put("formAction", "updateProfile");
        request.parameters.put("userName", "새 이름");
        ResponseFixture response = new ResponseFixture();

        new MyPageServlet(userDAO).doPost(
                request.proxy(), response.proxy());

        assertEquals("기존 이름", SessionPrincipal.from(
                request.session).getUserName());
        assertTrue(response.redirect.startsWith(
                "/frog2/mypage?action=editProfile&_flash="));
        assertFlash(
                request,
                response,
                "프로필 업데이트에 실패했습니다.",
                "error");
    }

    @Test
    void passwordValidationFailureRedirectsWithoutAuthentication()
            throws Exception {
        StubUserDAO userDAO = new StubUserDAO();
        RequestFixture request = passwordRequest("current", "short", "short");
        ResponseFixture response = new ResponseFixture();

        new MyPageServlet(userDAO).doPost(
                request.proxy(), response.proxy());

        assertEquals(0, userDAO.authenticationCalls);
        assertEquals(0, userDAO.updatePasswordCalls);
        assertTrue(response.redirect.startsWith(
                "/frog2/mypage?action=changePassword&_flash="));
        assertFalse(response.redirect.contains("current"));
        assertFalse(response.redirect.contains("short"));
        assertFlash(
                request,
                response,
                "새 비밀번호는 최소 8자 이상이어야 합니다.",
                "error");
    }

    @Test
    void wrongCurrentPasswordRedirectsWithoutWritingPassword()
            throws Exception {
        StubUserDAO userDAO = new StubUserDAO();
        RequestFixture request = passwordRequest(
                "wrong-password", "new-password", "new-password");
        ResponseFixture response = new ResponseFixture();

        new MyPageServlet(userDAO).doPost(
                request.proxy(), response.proxy());

        assertEquals(1, userDAO.authenticationCalls);
        assertEquals(0, userDAO.updatePasswordCalls);
        assertTrue(response.redirect.startsWith(
                "/frog2/mypage?action=changePassword&_flash="));
        assertFlash(
                request,
                response,
                "현재 비밀번호가 올바르지 않습니다.",
                "error");
    }

    @Test
    void passwordSuccessRedirectsToGetWithoutExposingPasswords()
            throws Exception {
        StubUserDAO userDAO = new StubUserDAO();
        userDAO.authenticatedUser = new UserDTO(
                "user-1", "", "기존 이름", "QA");
        userDAO.updatePasswordResult = true;
        RequestFixture request = passwordRequest(
                "old-password", "new-password", "new-password");
        ResponseFixture response = new ResponseFixture();

        new MyPageServlet(userDAO).doPost(
                request.proxy(), response.proxy());

        assertEquals("old-password", userDAO.authenticatedPassword);
        assertEquals("new-password", userDAO.updatedPassword);
        assertTrue(response.redirect.startsWith("/frog2/mypage?_flash="));
        assertFalse(response.redirect.contains("old-password"));
        assertFalse(response.redirect.contains("new-password"));
        assertFlash(
                request,
                response,
                "비밀번호가 성공적으로 변경되었습니다.",
                "success");
    }

    @Test
    void passwordWriteFailureRedirectsBackToPasswordForm()
            throws Exception {
        StubUserDAO userDAO = new StubUserDAO();
        userDAO.authenticatedUser = new UserDTO(
                "user-1", "", "기존 이름", "QA");
        RequestFixture request = passwordRequest(
                "old-password", "new-password", "new-password");
        ResponseFixture response = new ResponseFixture();

        new MyPageServlet(userDAO).doPost(
                request.proxy(), response.proxy());

        assertEquals(1, userDAO.updatePasswordCalls);
        assertTrue(response.redirect.startsWith(
                "/frog2/mypage?action=changePassword&_flash="));
        assertFlash(
                request,
                response,
                "비밀번호 변경에 실패했습니다.",
                "error");
    }

    private static RequestFixture passwordRequest(
            String currentPassword,
            String newPassword,
            String confirmPassword) {
        RequestFixture request = new RequestFixture();
        request.parameters.put("formAction", "updatePassword");
        request.parameters.put("currentPassword", currentPassword);
        request.parameters.put("newPassword", newPassword);
        request.parameters.put("confirmPassword", confirmPassword);
        return request;
    }

    private static void assertFlash(
            RequestFixture request,
            ResponseFixture response,
            String expectedMessage,
            String expectedType) {
        String token = response.redirect.substring(
                response.redirect.indexOf("_flash=") + "_flash=".length());
        request.parameters.put(FlashMessage.PARAMETER_NAME, token);
        FlashMessage.expose(request.proxy());
        assertEquals(expectedMessage, request.attributes.get("message"));
        assertEquals(expectedType, request.attributes.get("messageType"));
    }

    private static final class StubUserDAO extends UserDAO {
        private boolean updateNameResult;
        private boolean updatePasswordResult;
        private UserDTO authenticatedUser;
        private int updateNameCalls;
        private int authenticationCalls;
        private int updatePasswordCalls;
        private String updatedUserName;
        private String authenticatedPassword;
        private String updatedPassword;

        @Override
        public boolean updateUserName(String userId, String userName) {
            updateNameCalls++;
            updatedUserName = userName;
            return updateNameResult;
        }

        @Override
        public UserDTO authenticateUser(String userId, String password) {
            authenticationCalls++;
            authenticatedPassword = password;
            return authenticatedUser;
        }

        @Override
        public boolean updatePassword(String userId, String newPassword) {
            updatePasswordCalls++;
            updatedPassword = newPassword;
            return updatePasswordResult;
        }
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, Object> sessionAttributes = new HashMap<>();
        private final HttpSession session;

        private RequestFixture() {
            sessionAttributes.put(
                    SessionPrincipal.SESSION_ATTRIBUTE,
                    new UserDTO("user-1", "", "기존 이름", "QA"));
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
