package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.model.UserDTO;
import com.company.model.UserVmHostDAO;
import com.company.model.UserVmHostDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserVmHostServletOwnershipTest {
    @Test
    void getIgnoresForeignRequestUserIdAndUsesOnlyTheSessionOwner()
            throws Exception {
        StubUserVmHostDAO dao = new StubUserVmHostDAO();
        UserVmHostServlet servlet = new UserVmHostServlet(dao);
        RequestFixture request = new RequestFixture();
        request.parameters.put("userId", "attacker-1");
        request.parameters.put("editIp", "192.168.40.10");
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals("owner-1", dao.lastEditOwnerId);
        assertEquals("owner-1", dao.lastListOwnerId);
        assertEquals("/vm_hosts/list.jsp", request.forwardedPath);
    }

    @Test
    void deleteIgnoresForeignRequestUserIdAndUsesOnlyTheSessionOwner()
            throws Exception {
        StubUserVmHostDAO dao = new StubUserVmHostDAO();
        UserVmHostServlet servlet = new UserVmHostServlet(dao);
        RequestFixture request = new RequestFixture();
        request.parameters.put("userId", "attacker-1");
        request.parameters.put("action", "delete");
        request.parameters.put("ip", "192.168.40.10");
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertEquals("owner-1", dao.lastDeleteOwnerId);
        assertEquals(
                "/frog2/vm-hosts?result=deleted",
                response.redirect);
    }

    private static final class StubUserVmHostDAO extends UserVmHostDAO {
        private String lastEditOwnerId;
        private String lastListOwnerId;
        private String lastDeleteOwnerId;

        @Override
        public UserVmHostDTO getHostByIpAndOwner(
                String ip, String ownerUserId) {
            lastEditOwnerId = ownerUserId;
            return null;
        }

        @Override
        public List<UserVmHostDTO> getActiveHostsByOwner(String ownerUserId) {
            lastListOwnerId = ownerUserId;
            return List.of();
        }

        @Override
        public boolean deleteByIpAndOwner(String ip, String ownerUserId) {
            lastDeleteOwnerId = ownerUserId;
            return true;
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
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "getContextPath" -> "/frog2";
                        case "getAttribute" -> attributes.get((String) args[0]);
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
                    "owner-1", "", "Same Name", "QA");
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
        private String redirect;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "sendRedirect" -> {
                            redirect = (String) args[0];
                            yield null;
                        }
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
        return 0;
    }
}
