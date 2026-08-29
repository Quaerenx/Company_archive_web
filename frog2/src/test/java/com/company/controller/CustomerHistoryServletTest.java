package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.customerhistory.CustomerHistoryCategory;
import com.company.customerhistory.CustomerHistoryDraft;
import com.company.customerhistory.CustomerHistoryRecord;
import com.company.customerhistory.CustomerHistoryRepository;
import com.company.customerhistory.CustomerHistoryStatus;
import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.UserDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomerHistoryServletTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void anonymousListRequestRedirectsBeforeReadingTheRepository()
            throws Exception {
        Path repositoryRoot = temporaryDirectory.resolve("history");
        CustomerHistoryServlet servlet = servlet(repositoryRoot);
        RequestFixture request = new RequestFixture(null);
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals("/frog2/login", response.redirect);
        assertNull(request.forwardedPath);
        assertFalse(Files.exists(repositoryRoot));
    }

    @Test
    void authenticatedListExposesFilteredRecordsAndStableUserId()
            throws Exception {
        Path repositoryRoot = temporaryDirectory.resolve("history");
        CustomerHistoryRepository repository =
                new CustomerHistoryRepository(repositoryRoot);
        CustomerHistoryRecord expected = repository.create(
                draft("테크핀 레이팅스", "개발 서버 복구"),
                "owner-1",
                "담당자");
        repository.create(
                draft("다른 고객사", "노드 증설"),
                "owner-2",
                "다른 담당자");
        CustomerHistoryServlet servlet = new CustomerHistoryServlet(
                repository, new StubCustomerDAO());
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("customerName", " 테크핀 레이팅스 ");
        request.parameters.put("category", "incident");
        request.parameters.put("q", " 복구 ");
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals(
                "/customer-history/customer_history_list.jsp",
                request.forwardedPath);
        assertEquals(1, request.attributes.get("totalCount"));
        assertEquals("owner-1", request.attributes.get("currentUserId"));
        assertEquals(true, request.attributes.get("hasActiveFilters"));
        assertEquals("테크핀 레이팅스",
                request.attributes.get("customerName"));
        List<?> records =
                (List<?>) request.attributes.get("historyRecords");
        assertEquals(1, records.size());
        assertEquals(expected.getId(),
                ((CustomerHistoryRecord) records.getFirst()).getId());
        assertNull(response.redirect);
    }

    @Test
    void matchingDisplayNameDoesNotGrantEditAccessToAnotherUser()
            throws Exception {
        Path repositoryRoot = temporaryDirectory.resolve("history");
        CustomerHistoryRepository repository =
                new CustomerHistoryRepository(repositoryRoot);
        CustomerHistoryRecord record = repository.create(
                draft("테크핀 레이팅스", "개발 서버 복구"),
                "owner-1",
                "같은 이름");
        CustomerHistoryServlet servlet = new CustomerHistoryServlet(
                repository, new StubCustomerDAO());
        RequestFixture request = new RequestFixture(
                new UserDTO("attacker-1", "", "같은 이름", "QA"));
        request.parameters.put("view", "edit");
        request.parameters.put("id", record.getId());
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.errorStatus);
        assertNull(request.forwardedPath);
        assertNull(response.redirect);
    }

    @Test
    void editFormPreservesOnlyNormalizedListState() throws Exception {
        Path repositoryRoot = temporaryDirectory.resolve("history");
        CustomerHistoryRepository repository =
                new CustomerHistoryRepository(repositoryRoot);
        CustomerHistoryRecord record = repository.create(
                draft("테크핀 레이팅스", "개발 서버 복구"),
                "owner-1",
                "담당자");
        CustomerHistoryServlet servlet = new CustomerHistoryServlet(
                repository, new StubCustomerDAO());
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("view", "edit");
        request.parameters.put("id", record.getId());
        request.parameters.put("returnCustomerName", " 테크핀 레이팅스 ");
        request.parameters.put("returnCategory", "INCIDENT");
        request.parameters.put("returnQ", " 복구 작업 ");
        request.parameters.put("returnPage", "4");
        request.parameters.put("returnUrl", "https://example.invalid/");

        servlet.doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals(
                "/customer-history/customer_history_form.jsp",
                request.forwardedPath);
        assertEquals("테크핀 레이팅스",
                request.attributes.get("formReturnCustomerName"));
        assertEquals("incident", request.attributes.get("formReturnCategory"));
        assertEquals("복구 작업", request.attributes.get("formReturnQ"));
        assertEquals(4, request.attributes.get("formReturnPage"));
        assertEquals(
                listUrl("테크핀 레이팅스", "incident", "복구 작업", 4),
                request.attributes.get("returnListUrl"));
        assertFalse(((String) request.attributes.get("returnListUrl"))
                .contains("example.invalid"));
    }

    @Test
    void invalidListStateFallsBackToTheHistoryRoot() throws Exception {
        CustomerHistoryServlet servlet = servlet(
                temporaryDirectory.resolve("history"));
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("view", "add");
        request.parameters.put("returnCustomerName", "x".repeat(201));
        request.parameters.put("returnCategory", "not-a-category");
        request.parameters.put("returnQ", "x".repeat(101));
        request.parameters.put("returnPage", "not-a-page");
        request.parameters.put("returnUrl", "https://example.invalid/");

        servlet.doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals("", request.attributes.get("formReturnCustomerName"));
        assertEquals("all", request.attributes.get("formReturnCategory"));
        assertEquals("", request.attributes.get("formReturnQ"));
        assertEquals(1, request.attributes.get("formReturnPage"));
        assertEquals(
                "/frog2/customer-history",
                request.attributes.get("returnListUrl"));
    }

    @Test
    void successfulUpdateReturnsToTheFilteredList() throws Exception {
        Path repositoryRoot = temporaryDirectory.resolve("history");
        CustomerHistoryRepository repository =
                new CustomerHistoryRepository(repositoryRoot);
        CustomerHistoryRecord record = repository.create(
                draft("테크핀 레이팅스", "개발 서버 복구"),
                "owner-1",
                "담당자");
        CustomerHistoryServlet servlet = new CustomerHistoryServlet(
                repository, new StubCustomerDAO());
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("action", "update");
        request.parameters.put("id", record.getId());
        putDraft(request, "개발 서버 복구 완료");
        putReturnState(request, 3);
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertTrue(response.redirect.startsWith(
                listUrl("테크핀 레이팅스", "incident", "복구", 3)
                        + "&_flash="));
        assertTrue(repository.findById(record.getId()).orElseThrow()
                .getTitle().contains("완료"));
    }

    @Test
    void successfulAddReturnsToTheFilteredList() throws Exception {
        Path repositoryRoot = temporaryDirectory.resolve("history");
        CustomerHistoryServlet servlet = servlet(repositoryRoot);
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("action", "add");
        putDraft(request, "개발 서버 복구");
        putReturnState(request, 2);
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertTrue(response.redirect.startsWith(
                listUrl("테크핀 레이팅스", "incident", "복구", 2)
                        + "&_flash="));
    }

    @Test
    void addFormUsesSeoulDateAtUtcMonthBoundary() throws Exception {
        Clock utcClockAtSeoulMidnight = Clock.fixed(
                Instant.parse("2026-08-31T15:00:00Z"),
                ZoneOffset.UTC);
        CustomerHistoryServlet servlet = new CustomerHistoryServlet(
                new CustomerHistoryRepository(
                        temporaryDirectory.resolve("history")),
                new StubCustomerDAO(),
                utcClockAtSeoulMidnight);
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("view", "add");

        servlet.doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals("2026-09-01",
                request.attributes.get("formWorkDate"));
    }

    private CustomerHistoryServlet servlet(Path repositoryRoot) {
        return new CustomerHistoryServlet(
                new CustomerHistoryRepository(repositoryRoot),
                new StubCustomerDAO());
    }

    private static CustomerHistoryDraft draft(
            String customerName, String title) {
        return new CustomerHistoryDraft(
                customerName,
                LocalDate.of(2026, 8, 19),
                CustomerHistoryCategory.INCIDENT,
                title,
                "서비스 정상화",
                CustomerHistoryStatus.COMPLETED);
    }

    private static UserDTO user(String userId) {
        return new UserDTO(userId, "", "담당자", "QA");
    }

    private static void putDraft(RequestFixture request, String title) {
        request.parameters.put("customerName", "테크핀 레이팅스");
        request.parameters.put("workDate", "2026-08-19");
        request.parameters.put("category", "incident");
        request.parameters.put("title", title);
        request.parameters.put("actionSummary", "서비스 정상화");
        request.parameters.put("status", "completed");
    }

    private static void putReturnState(RequestFixture request, int page) {
        request.parameters.put("returnCustomerName", "테크핀 레이팅스");
        request.parameters.put("returnCategory", "incident");
        request.parameters.put("returnQ", "복구");
        request.parameters.put("returnPage", Integer.toString(page));
    }

    private static String listUrl(
            String customerName, String category, String query, int page) {
        return "/frog2/customer-history?customerName=" + encode(customerName)
                + "&category=" + encode(category)
                + "&q=" + encode(query)
                + "&page=" + page;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final class StubCustomerDAO extends CustomerDAO {
        @Override
        public List<CustomerDTO> getMaintenanceCustomers(
                String sortField, String sortDirection) {
            CustomerDTO customer = new CustomerDTO();
            customer.setCustomerName("테크핀 레이팅스");
            return List.of(customer);
        }

        @Override
        public boolean isActiveMaintenanceCustomer(String customerName) {
            return "테크핀 레이팅스".equals(customerName);
        }
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpSession session;
        private String forwardedPath;

        private RequestFixture(UserDTO user) {
            session = user == null ? null : session(user);
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
                        case "getRequestURI" ->
                                "/frog2/customer-history";
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

        private static HttpSession session(UserDTO user) {
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
        private int errorStatus = HttpServletResponse.SC_OK;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "sendRedirect" -> {
                            redirect = (String) args[0];
                            yield null;
                        }
                        case "sendError" -> {
                            errorStatus = (Integer) args[0];
                            yield null;
                        }
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

}
