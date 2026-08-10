package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.TroubleshootingDAO;
import com.company.model.TroubleshootingDTO;
import com.company.model.PageResult;
import com.company.model.UserDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TroubleshootingServletAuthorizationTest {
    @Test
    void listUsesBoundedPageAndExposesPaginationMetadata()
            throws Exception {
        StubTroubleshootingDAO dao = new StubTroubleshootingDAO();
        dao.page = new PageResult<>(
                List.of(record("owner-1", "Owner")),
                41,
                2,
                20);
        TroubleshootingServlet servlet = new TroubleshootingServlet(dao);
        RequestFixture request =
                new RequestFixture(user("owner-1", "Owner"), "GET");
        request.parameters.put("view", "list");
        request.parameters.put("q", "  connection  ");
        request.parameters.put("scope", "content");
        request.parameters.put("page", "2");
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals("connection", dao.lastQuery);
        assertTrue(dao.lastIncludeContent);
        assertEquals(2, dao.lastPage);
        assertEquals(20, dao.lastPageSize);
        assertEquals(
                "/troubleshooting/troubleshooting_list.jsp",
                request.forwardedPath);
        assertEquals(41, request.attributes.get("totalCount"));
        assertEquals(2, request.attributes.get("currentPage"));
        assertEquals(3, request.attributes.get("totalPages"));
        assertEquals("content", request.attributes.get("searchScope"));
        assertEquals(dao.page.items(),
                request.attributes.get("troubleshootingList"));
    }

    @Test
    void sameDisplayNameDoesNotGrantManageActionsToAnotherUser() throws Exception {
        StubTroubleshootingDAO dao = new StubTroubleshootingDAO();
        dao.troubleshooting = record("owner-1", "Same Name");
        TroubleshootingServlet servlet = new TroubleshootingServlet(dao);

        RequestFixture attacker =
                new RequestFixture(user("attacker-1", "Same Name"), "GET");
        attacker.parameters.put("view", "view");
        attacker.parameters.put("id", "7");
        servlet.doGet(attacker.proxy(), new ResponseFixture().proxy());

        assertEquals(
                "/troubleshooting/troubleshooting_view.jsp",
                attacker.forwardedPath);
        assertEquals(
                Boolean.FALSE,
                attacker.attributes.get("canManageTroubleshooting"));

        RequestFixture owner =
                new RequestFixture(user("owner-1", "Renamed Owner"), "GET");
        owner.parameters.put("view", "view");
        owner.parameters.put("id", "7");
        servlet.doGet(owner.proxy(), new ResponseFixture().proxy());

        assertEquals(
                Boolean.TRUE,
                owner.attributes.get("canManageTroubleshooting"));
    }

    @Test
    void nonOwnerCannotOpenTheEditFormEvenWhenNamesMatch() throws Exception {
        StubTroubleshootingDAO dao = new StubTroubleshootingDAO();
        dao.troubleshooting = record("owner-1", "Same Name");
        TroubleshootingServlet servlet = new TroubleshootingServlet(dao);
        RequestFixture request =
                new RequestFixture(user("attacker-1", "Same Name"), "GET");
        request.parameters.put("view", "edit");
        request.parameters.put("id", "7");
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertNull(request.forwardedPath);
        assertEquals(
                "troubleshooting?view=view&id=7",
                response.redirect);
        assertEquals(
                "수정 권한이 없습니다.",
                request.sessionAttributes.get("error"));
    }

    @Test
    void postMutationsPassOnlyTheSessionUserIdToOwnerScopedDaoMethods()
            throws Exception {
        StubTroubleshootingDAO dao = new StubTroubleshootingDAO();
        TroubleshootingServlet servlet = new TroubleshootingServlet(dao);

        RequestFixture update =
                new RequestFixture(user("attacker-1", "Same Name"), "POST");
        update.parameters.put("action", "update");
        update.parameters.put("id", "7");
        update.parameters.put("title", "Updated issue");
        update.parameters.put("customer_name", "Acme");
        ResponseFixture updateResponse = new ResponseFixture();
        servlet.doPost(update.proxy(), updateResponse.proxy());

        assertEquals("attacker-1", dao.lastUpdateOwnerId);
        assertEquals(
                "troubleshooting?view=list",
                updateResponse.redirect);
        assertTrue(update.sessionAttributes.containsKey("error"));

        RequestFixture delete =
                new RequestFixture(user("attacker-1", "Same Name"), "POST");
        delete.parameters.put("action", "delete");
        delete.parameters.put("id", "7");
        ResponseFixture deleteResponse = new ResponseFixture();
        servlet.doPost(delete.proxy(), deleteResponse.proxy());

        assertEquals("attacker-1", dao.lastDeleteOwnerId);
        assertEquals(
                "troubleshooting?view=list",
                deleteResponse.redirect);
        assertTrue(delete.sessionAttributes.containsKey("error"));
    }

    @Test
    void legitimateOwnerKeepsTheExistingSuccessfulUpdateFlow()
            throws Exception {
        StubTroubleshootingDAO dao = new StubTroubleshootingDAO();
        dao.mutationSucceeds = true;
        TroubleshootingServlet servlet = new TroubleshootingServlet(dao);
        RequestFixture request =
                new RequestFixture(user("owner-1", "Renamed Owner"), "POST");
        request.parameters.put("action", "update");
        request.parameters.put("id", "7");
        request.parameters.put("title", "Updated issue");
        request.parameters.put("customer_name", "Acme");
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertEquals("owner-1", dao.lastUpdateOwnerId);
        assertEquals(
                "troubleshooting?view=view&id=7",
                response.redirect);
        assertTrue(request.sessionAttributes.containsKey("message"));
        assertFalse(request.sessionAttributes.containsKey("error"));
    }

    private static TroubleshootingDTO record(
            String creatorUserId, String creatorName) {
        TroubleshootingDTO troubleshooting = new TroubleshootingDTO();
        troubleshooting.setId(7);
        troubleshooting.setTitle("Connection issue");
        troubleshooting.setCreatorUserId(creatorUserId);
        troubleshooting.setCreator(creatorName);
        return troubleshooting;
    }

    private static UserDTO user(String userId, String userName) {
        return new UserDTO(userId, "", userName, "QA");
    }

    private static final class StubTroubleshootingDAO
            extends TroubleshootingDAO {
        private TroubleshootingDTO troubleshooting;
        private PageResult<TroubleshootingDTO> page =
                new PageResult<>(List.of(), 0, 1, 20);
        private String lastQuery;
        private int lastPage;
        private int lastPageSize;
        private boolean lastIncludeContent;
        private String lastUpdateOwnerId;
        private String lastDeleteOwnerId;
        private boolean mutationSucceeds;

        @Override
        public TroubleshootingDTO getTroubleshootingById(int id) {
            return troubleshooting;
        }

        @Override
        public PageResult<TroubleshootingDTO> getTroubleshootingPage(
                String query, int requestedPage, int pageSize) {
            return getTroubleshootingPage(
                    query, false, requestedPage, pageSize);
        }

        @Override
        public PageResult<TroubleshootingDTO> getTroubleshootingPage(
                String query,
                boolean includeContent,
                int requestedPage,
                int pageSize) {
            lastQuery = query;
            lastIncludeContent = includeContent;
            lastPage = requestedPage;
            lastPageSize = pageSize;
            return page;
        }

        @Override
        public boolean updateTroubleshootingForOwner(
                TroubleshootingDTO record, String creatorUserId) {
            lastUpdateOwnerId = creatorUserId;
            return mutationSucceeds;
        }

        @Override
        public boolean deleteTroubleshootingForOwner(
                int id, String creatorUserId) {
            lastDeleteOwnerId = creatorUserId;
            return mutationSucceeds;
        }
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, Object> sessionAttributes = new HashMap<>();
        private final HttpSession session;
        private final String method;
        private String forwardedPath;

        private RequestFixture(UserDTO user, String method) {
            this.method = method;
            sessionAttributes.put("user", user);
            session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[] {HttpSession.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getAttribute" ->
                                sessionAttributes.get((String) args[0]);
                        case "setAttribute" -> {
                            sessionAttributes.put(
                                    (String) args[0], args[1]);
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
                        case "getRequestURI" -> "/frog2/troubleshooting";
                        case "getMethod" -> method;
                        case "getAttribute" ->
                                attributes.get((String) args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "removeAttribute" -> {
                            attributes.remove((String) args[0]);
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
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == float.class) {
            return 0F;
        }
        return 0;
    }
}
