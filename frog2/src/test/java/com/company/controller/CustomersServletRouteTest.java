package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.company.model.UserDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CustomersServletRouteTest {
    @Test
    void unauthenticatedRequestsKeepLoginRedirectContract() throws Exception {
        RequestFixture getRequest = new RequestFixture(false);
        ResponseFixture getResponse = new ResponseFixture();
        new CustomersServlet().doGet(getRequest.proxy(), getResponse.proxy());

        RequestFixture postRequest = new RequestFixture(false);
        postRequest.parameters.put("action", "unknown");
        ResponseFixture postResponse = new ResponseFixture();
        new CustomersServlet().doPost(postRequest.proxy(), postResponse.proxy());

        assertEquals("/frog2/login", getResponse.redirect);
        assertEquals("/frog2/login", postResponse.redirect);
    }

    @Test
    void addViewRemainsAndRemovedSupportViewRedirectsToList() throws Exception {
        RequestFixture addRequest = authenticatedRequest("add");
        ResponseFixture addResponse = new ResponseFixture();
        new CustomersServlet().doGet(addRequest.proxy(), addResponse.proxy());

        RequestFixture supportRequest = authenticatedRequest("support");
        ResponseFixture supportResponse = new ResponseFixture();
        new CustomersServlet().doGet(supportRequest.proxy(), supportResponse.proxy());

        assertEquals("/customers/customers_add.jsp", addRequest.forwardedPath);
        assertNull(supportRequest.forwardedPath);
        assertNull(addResponse.redirect);
        assertEquals("customers?view=list", supportResponse.redirect);
    }

    @Test
    void missingCustomerIdentifiersKeepListRedirectContract() throws Exception {
        for (String view : new String[] {"detail", "edit", "editDetail"}) {
            RequestFixture request = authenticatedRequest(view);
            ResponseFixture response = new ResponseFixture();

            new CustomersServlet().doGet(request.proxy(), response.proxy());

            assertEquals("customers?view=list", response.redirect, view);
            assertNull(request.forwardedPath, view);
        }
    }

    @Test
    void unknownViewAndPostActionKeepListRedirectContract() throws Exception {
        RequestFixture getRequest = authenticatedRequest("not-a-view");
        ResponseFixture getResponse = new ResponseFixture();
        new CustomersServlet().doGet(getRequest.proxy(), getResponse.proxy());

        RequestFixture postRequest = new RequestFixture(true);
        postRequest.parameters.put("action", "not-an-action");
        ResponseFixture postResponse = new ResponseFixture();
        new CustomersServlet().doPost(postRequest.proxy(), postResponse.proxy());

        assertEquals("customers?view=list", getResponse.redirect);
        assertEquals("customers?view=list", postResponse.redirect);
    }

    private static RequestFixture authenticatedRequest(String view) {
        RequestFixture request = new RequestFixture(true);
        request.parameters.put("view", view);
        return request;
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpSession session;
        private String forwardedPath;

        private RequestFixture(boolean authenticated) {
            UserDTO user = authenticated ? new UserDTO("tester", "", "Tester", "QA") : null;
            session = authenticated ? session(user) : null;
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getSession" -> session;
                        case "getContextPath" -> "/frog2";
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getRequestDispatcher" -> dispatcher((String) args[0]);
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
                        return null;
                    });
        }

        private static HttpSession session(UserDTO user) {
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
        private String redirect;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> {
                        if ("sendRedirect".equals(call.getName())) {
                            redirect = (String) args[0];
                            return null;
                        }
                        return defaultValue(call.getReturnType());
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
        return 0;
    }
}
