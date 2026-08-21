package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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

    private static final class StubCustomerDAO extends CustomerDAO {
        @Override
        public List<CustomerDTO> getMaintenanceCustomers(
                String sortField, String sortDirection) {
            CustomerDTO customer = new CustomerDTO();
            customer.setCustomerName("테크핀 레이팅스");
            return List.of(customer);
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
