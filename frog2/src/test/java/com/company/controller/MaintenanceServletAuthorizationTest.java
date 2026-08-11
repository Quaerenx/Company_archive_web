package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.PageResult;
import com.company.model.UserDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaintenanceServletAuthorizationTest {
    @Test
    void historyDefaultsToTwentyRecordsAndSharesThePageWithTheChart()
            throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        MaintenanceRecordDTO history = record("owner-1");
        history.setInspectionDate(Date.valueOf("2026-08-10"));
        history.setLicenseSizeGb("1000");
        history.setLicenseUsageSize("500");
        history.setLicenseUsagePct("50");
        dao.historyPage = new PageResult<>(
                List.of(history), 41, 1, 20);
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        MaintenanceServlet servlet = new MaintenanceServlet(
                dao, customerDAO);
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("view", "history");
        request.parameters.put("customerName", "Acme");
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals("Acme", dao.lastHistoryCustomer);
        assertEquals(1, dao.lastHistoryPage);
        assertEquals(20, dao.lastHistoryPageSize);
        assertEquals(
                "/maintenance/maintenance_history.jsp",
                request.forwardedPath);
        assertEquals(dao.historyPage.items(),
                request.attributes.get("records"));
        assertEquals(41, request.attributes.get("totalCount"));
        assertEquals(1, request.attributes.get("currentPage"));
        assertEquals(3, request.attributes.get("totalPages"));
        assertEquals(20, request.attributes.get("pageSize"));
        assertTrue(history.getLicenseSummary() != null
                && !history.getLicenseSummary().isBlank());
        assertEquals(1,
                ((List<?>) request.attributes.get("usageSeries")).size());
    }

    @Test
    void invalidHistoryPageReturnsJsonBadRequestBeforeDaoUse()
            throws Exception {
        for (String invalidPage : List.of("0", "-1", "abc")) {
            StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
            MaintenanceServlet servlet = new MaintenanceServlet(
                    dao, new StubCustomerDAO());
            RequestFixture request = new RequestFixture(user("owner-1"));
            request.accept = "application/json";
            request.parameters.put("view", "history");
            request.parameters.put("customerName", "Acme");
            request.parameters.put("historyPage", invalidPage);
            ResponseFixture response = new ResponseFixture();

            servlet.doGet(request.proxy(), response.proxy());

            assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
            assertTrue(response.body.toString().contains(
                    "\"code\":\"invalid_history_page\""));
            assertEquals(0, dao.historyReads);
        }
    }

    @Test
    void nonOwnerCannotOpenEditForm() throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        dao.record = record("owner-1");
        MaintenanceServlet servlet = new MaintenanceServlet(dao);
        RequestFixture request = new RequestFixture(user("attacker-1"));
        request.parameters.put("view", "edit");
        request.parameters.put("id", "17");
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertNull(request.forwardedPath);
        assertEquals(
                "maintenance?view=history&customerName=Acme",
                response.redirect);
        assertEquals(
                "수정 권한이 없습니다.",
                request.sessionAttributes.get("error"));
    }

    @Test
    void postMutationsUseOnlySessionUserId() throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        MaintenanceServlet servlet = new MaintenanceServlet(dao);

        RequestFixture update = new RequestFixture(user("attacker-1"));
        update.parameters.put("action", "update");
        update.parameters.put("maintenance_id", "17");
        update.parameters.put("customer_name", "Acme");
        update.parameters.put("inspector_name", "Same Name");
        update.parameters.put("inspection_date", "2026-08-03");
        ResponseFixture updateResponse = new ResponseFixture();

        servlet.doPost(update.proxy(), updateResponse.proxy());

        assertEquals("attacker-1", dao.lastUpdateOwnerId);
        assertEquals(
                "maintenance?view=history&customerName=Acme",
                updateResponse.redirect);
        assertTrue(update.sessionAttributes.containsKey("error"));

        RequestFixture delete = new RequestFixture(user("attacker-1"));
        delete.parameters.put("action", "delete");
        delete.parameters.put("maintenance_id", "17");
        delete.parameters.put("customer_name", "Acme");
        ResponseFixture deleteResponse = new ResponseFixture();

        servlet.doPost(delete.proxy(), deleteResponse.proxy());

        assertEquals("attacker-1", dao.lastDeleteOwnerId);
        assertEquals(
                "maintenance?view=history&customerName=Acme",
                deleteResponse.redirect);
        assertTrue(delete.sessionAttributes.containsKey("error"));
    }

    private static MaintenanceRecordDTO record(String creatorUserId) {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setMaintenanceId(17L);
        record.setCreatorUserId(creatorUserId);
        record.setCustomerName("Acme");
        return record;
    }

    private static UserDTO user(String userId) {
        return new UserDTO(userId, "", "Same Name", "QA");
    }

    private static final class StubMaintenanceRecordDAO
            extends MaintenanceRecordDAO {
        private MaintenanceRecordDTO record;
        private String lastUpdateOwnerId;
        private String lastDeleteOwnerId;
        private PageResult<MaintenanceRecordDTO> historyPage =
                new PageResult<>(List.of(), 0, 1, 20);
        private String lastHistoryCustomer;
        private int lastHistoryPage;
        private int lastHistoryPageSize;
        private int historyReads;

        @Override
        public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByCustomer(
                String customerName, int page, int pageSize) {
            historyReads++;
            lastHistoryCustomer = customerName;
            lastHistoryPage = page;
            lastHistoryPageSize = pageSize;
            return historyPage;
        }

        @Override
        public MaintenanceRecordDTO getMaintenanceRecordById(
                Long maintenanceId) {
            return record;
        }

        @Override
        public boolean updateMaintenanceRecordForOwner(
                MaintenanceRecordDTO record, String creatorUserId) {
            lastUpdateOwnerId = creatorUserId;
            return false;
        }

        @Override
        public boolean deleteMaintenanceRecordForOwner(
                Long maintenanceId, String creatorUserId) {
            lastDeleteOwnerId = creatorUserId;
            return false;
        }
    }

    private static final class StubCustomerDAO extends CustomerDAO {
        @Override
        public CustomerDTO getCustomerByName(String customerName) {
            CustomerDTO customer = new CustomerDTO();
            customer.setCustomerName(customerName);
            return customer;
        }
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, Object> sessionAttributes = new HashMap<>();
        private final HttpSession session;
        private String forwardedPath;
        private String accept;

        private RequestFixture(UserDTO user) {
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
                        case "getRequestURI" -> "/frog2/maintenance";
                        case "getMethod" -> "GET";
                        case "getHeader" -> "Accept".equals(args[0])
                                ? accept
                                : null;
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
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private String redirect;
        private int status = HttpServletResponse.SC_OK;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "sendRedirect" -> {
                            redirect = (String) args[0];
                            yield null;
                        }
                        case "sendError", "setStatus" -> {
                            status = (Integer) args[0];
                            yield null;
                        }
                        case "getWriter" -> writer;
                        case "isCommitted" -> false;
                        case "resetBuffer" -> {
                            body.getBuffer().setLength(0);
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
        return 0;
    }
}
