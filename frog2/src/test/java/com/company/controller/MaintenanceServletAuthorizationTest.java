package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.MaintenanceHistoryFilter;
import com.company.model.MaintenanceFormHistoryContext;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        history.setLicenseSizeGb("4TB");
        history.setLicenseUsageSize("2TB");
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
        List<?> historyRows =
                (List<?>) request.attributes.get("historyRows");
        assertEquals(1, historyRows.size());
        MaintenanceHistoryRowView row =
                (MaintenanceHistoryRowView) historyRows.get(0);
        assertEquals(history, row.getRecord());
        assertEquals("2", row.getUsedTerabytes());
        assertEquals("4", row.getCapacityTerabytes());
        assertEquals(new java.math.BigDecimal("50.0"),
                row.getUsagePercentage());
        assertEquals("-", row.getDeltaLabel());
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
    void historyFiltersAreNormalizedAndSharedWithTheView() throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        MaintenanceServlet servlet = new MaintenanceServlet(
                dao, new StubCustomerDAO());
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("view", "history");
        request.parameters.put("customerName", "Acme");
        request.parameters.put("historyYear", " 2026 ");
        request.parameters.put("historyVersion", " 23.4 ");
        request.parameters.put("historyQuery", " 김동완 ");
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals(2026, dao.lastHistoryFilter.year());
        assertEquals("23.4", dao.lastHistoryFilter.version());
        assertEquals("김동완", dao.lastHistoryFilter.query());
        assertEquals(2026, request.attributes.get("historyYear"));
        assertEquals("23.4", request.attributes.get("historyVersion"));
        assertEquals("김동완", request.attributes.get("historyQuery"));
        assertEquals(true,
                request.attributes.get("historyFiltersActive"));
        assertEquals(
                "/maintenance/maintenance_history.jsp",
                request.forwardedPath);
    }

    @Test
    void invalidHistoryFilterReturnsJsonBadRequestBeforeDaoUse()
            throws Exception {
        for (Map.Entry<String, String> invalid : Map.of(
                "historyYear", "twenty",
                "historyVersion", "v".repeat(65),
                "historyQuery", "q".repeat(121)).entrySet()) {
            StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
            MaintenanceServlet servlet = new MaintenanceServlet(
                    dao, new StubCustomerDAO());
            RequestFixture request = new RequestFixture(user("owner-1"));
            request.accept = "application/json";
            request.parameters.put("view", "history");
            request.parameters.put("customerName", "Acme");
            request.parameters.put(invalid.getKey(), invalid.getValue());
            ResponseFixture response = new ResponseFixture();

            servlet.doGet(request.proxy(), response.proxy());

            assertEquals(
                    HttpServletResponse.SC_BAD_REQUEST,
                    response.status);
            assertTrue(response.body.toString().contains(
                    "\"code\":\"invalid_history_filter\""));
            assertEquals(0, dao.historyReads);
        }
    }

    @Test
    void nonOwnerCannotOpenEditForm() throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        dao.record = record("owner-1");
        MaintenanceServlet servlet = new MaintenanceServlet(
                dao, new StubCustomerDAO());
        RequestFixture request = new RequestFixture(user("attacker-1"));
        request.parameters.put("view", "edit");
        request.parameters.put("id", "17");
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertNull(request.forwardedPath);
        assertTrue(response.redirect.startsWith(
                "maintenance?view=history&customerName=Acme&_flash="));
    }

    @Test
    void editRejectsMissingOrMalformedMaintenanceIdBeforeDaoUse()
            throws Exception {
        for (String invalidId : new String[] {null, "", "abc", "0", "-1"}) {
            StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
            MaintenanceServlet servlet = new MaintenanceServlet(
                    dao, new StubCustomerDAO());
            RequestFixture request = new RequestFixture(user("owner-1"));
            request.parameters.put("view", "edit");
            if (invalidId != null) {
                request.parameters.put("id", invalidId);
            }
            request.accept = "application/json";
            ResponseFixture response = new ResponseFixture();

            servlet.doGet(request.proxy(), response.proxy());

            assertEquals(
                    HttpServletResponse.SC_BAD_REQUEST,
                    response.status);
            assertTrue(response.body.toString().contains(
                    "\"code\":\"invalid_maintenance_id\""));
            assertEquals(0, dao.recordReads);
            assertNull(response.redirect);
        }
    }

    @Test
    void addFormIsServerRenderedWithCustomerDefaultsAndPreviousRecord()
            throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        MaintenanceRecordDTO previous = record("owner-1");
        previous.setInspectionDate(Date.valueOf("2026-07-20"));
        previous.setVerticaVersion("23.4");
        previous.setLicenseSizeGb("25TB");
        dao.formContext = new MaintenanceFormHistoryContext(previous, null);
        MaintenanceServlet servlet = new MaintenanceServlet(
                dao, new StubCustomerDAO());
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("view", "add");
        request.parameters.put("customerName", "Acme");
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals(
                "/maintenance/maintenance_add.jsp",
                request.forwardedPath);
        MaintenanceRecordDTO formRecord = (MaintenanceRecordDTO)
                request.attributes.get("formRecord");
        assertEquals("Acme", formRecord.getCustomerName());
        assertEquals("Alice", formRecord.getInspectorName());
        assertEquals("23.4.0-13", formRecord.getVerticaVersion());
        assertEquals("25TB", formRecord.getLicenseSizeGb());
        assertTrue(formRecord.getInspectionDate() != null);
        assertEquals(previous,
                request.attributes.get("previousMaintenanceRecord"));
        assertEquals(true,
                request.attributes.get("formCustomerFixed"));
    }

    @Test
    void addFormUsesSeoulDateAtUtcMonthBoundary() throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        Clock utcClockAtSeoulMidnight = Clock.fixed(
                Instant.parse("2026-08-31T15:00:00Z"),
                ZoneOffset.UTC);
        MaintenanceServlet servlet = new MaintenanceServlet(
                dao, new StubCustomerDAO(), utcClockAtSeoulMidnight);
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("view", "add");

        servlet.doGet(request.proxy(), new ResponseFixture().proxy());

        MaintenanceRecordDTO formRecord = (MaintenanceRecordDTO)
                request.attributes.get("formRecord");
        assertEquals(Date.valueOf("2026-09-01"),
                formRecord.getInspectionDate());
    }

    @Test
    void invalidAddPreservesTheSubmissionAndDoesNotCallTheDaoWrite()
            throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        MaintenanceServlet servlet = new MaintenanceServlet(
                dao, new StubCustomerDAO());
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("action", "add");
        request.parameters.put("customer_name", "Acme");
        request.parameters.put("inspector_name", "Alice");
        request.parameters.put("inspection_date", "2026-02-30");
        request.parameters.put("note", "작성 중인 긴 점검 메모");
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertEquals(
                "/maintenance/maintenance_add.jsp",
                request.forwardedPath);
        assertFalse(dao.addCalled);
        MaintenanceRecordDTO formRecord = (MaintenanceRecordDTO)
                request.attributes.get("formRecord");
        assertEquals("작성 중인 긴 점검 메모", formRecord.getNote());
        Map<?, ?> errors = (Map<?, ?>)
                request.attributes.get("fieldErrors");
        assertTrue(errors.containsKey("inspection_date"));
    }

    @Test
    void formContextReturnsPreviousDefaultsAndDuplicateWarningJson()
            throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        MaintenanceRecordDTO previous = record("owner-1");
        previous.setInspectionDate(Date.valueOf("2026-07-20"));
        previous.setVerticaVersion("23.4");
        previous.setLicenseSizeGb("25TB");
        MaintenanceRecordDTO duplicate = record("owner-1");
        duplicate.setMaintenanceId(18L);
        duplicate.setInspectionDate(Date.valueOf("2026-08-03"));
        dao.formContext = new MaintenanceFormHistoryContext(
                previous, duplicate);
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        MaintenanceServlet servlet = new MaintenanceServlet(
                dao, customerDAO);
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("view", "formContext");
        request.parameters.put("customerName", "Acme");
        request.parameters.put("inspectionDate", "2026-08-12");
        request.accept = "application/json";
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertTrue(response.body.toString().contains(
                "\"defaultInspector\":\"Alice\""));
        assertTrue(response.body.toString().contains(
                "\"defaultLicenseSize\":\"25\""));
        assertTrue(response.body.toString().contains(
                "\"previous\":{"));
        assertTrue(response.body.toString().contains(
                "\"duplicate\":{"));
        assertNull(request.forwardedPath);
        assertEquals(1, customerDAO.customerReads);
        assertEquals(0, customerDAO.allCustomerReads);
    }

    @Test
    void formContextRejectsAnActiveNonMaintenanceCustomer()
            throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        customerDAO.customer.setCustomerType("일반 고객사");
        MaintenanceServlet servlet = new MaintenanceServlet(
                dao, customerDAO);
        RequestFixture request = new RequestFixture(user("owner-1"));
        request.parameters.put("view", "formContext");
        request.parameters.put("customerName", "Acme");
        request.parameters.put("inspectionDate", "2026-08-12");
        request.accept = "application/json";
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertTrue(response.body.toString().contains(
                "\"code\":\"invalid_maintenance_context\""));
        assertEquals(1, customerDAO.customerReads);
        assertEquals(0, customerDAO.allCustomerReads);
        assertNull(dao.lastFormContextCustomer);
    }

    @Test
    void postMutationsUseOnlySessionUserId() throws Exception {
        StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
        dao.record = record("attacker-1");
        dao.record.setInspectorName("Same Name");
        MaintenanceServlet servlet = new MaintenanceServlet(
                dao, new StubCustomerDAO());

        RequestFixture update = new RequestFixture(user("attacker-1"));
        update.parameters.put("action", "update");
        update.parameters.put("maintenance_id", "17");
        update.parameters.put("customer_name", "Acme");
        update.parameters.put("inspector_name", "Same Name");
        update.parameters.put("inspection_date", "2026-08-03");
        ResponseFixture updateResponse = new ResponseFixture();

        servlet.doPost(update.proxy(), updateResponse.proxy());

        assertEquals("attacker-1", dao.lastUpdateOwnerId);
        assertTrue(updateResponse.redirect.startsWith(
                "maintenance?view=history&customerName=Acme&_flash="));

        RequestFixture delete = new RequestFixture(user("attacker-1"));
        delete.parameters.put("action", "delete");
        delete.parameters.put("maintenance_id", "17");
        delete.parameters.put("customer_name", "Acme");
        ResponseFixture deleteResponse = new ResponseFixture();

        servlet.doPost(delete.proxy(), deleteResponse.proxy());

        assertEquals("attacker-1", dao.lastDeleteOwnerId);
        assertTrue(deleteResponse.redirect.startsWith(
                "maintenance?view=history&customerName=Acme&_flash="));
    }

    @Test
    void updateRejectsMissingOrMalformedMaintenanceIdBeforeDaoUse()
            throws Exception {
        for (String invalidId : new String[] {null, "", "abc", "0", "-1"}) {
            StubMaintenanceRecordDAO dao = new StubMaintenanceRecordDAO();
            MaintenanceServlet servlet = new MaintenanceServlet(
                    dao, new StubCustomerDAO());
            RequestFixture request = new RequestFixture(user("owner-1"));
            request.parameters.put("action", "update");
            if (invalidId != null) {
                request.parameters.put("maintenance_id", invalidId);
            }
            request.accept = "application/json";
            ResponseFixture response = new ResponseFixture();

            servlet.doPost(request.proxy(), response.proxy());

            assertEquals(
                    HttpServletResponse.SC_BAD_REQUEST,
                    response.status);
            assertTrue(response.body.toString().contains(
                    "\"code\":\"invalid_maintenance_id\""));
            assertEquals(0, dao.recordReads);
            assertNull(response.redirect);
        }
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
        private int recordReads;
        private String lastUpdateOwnerId;
        private String lastDeleteOwnerId;
        private PageResult<MaintenanceRecordDTO> historyPage =
                new PageResult<>(List.of(), 0, 1, 20);
        private String lastHistoryCustomer;
        private int lastHistoryPage;
        private int lastHistoryPageSize;
        private MaintenanceHistoryFilter lastHistoryFilter =
                MaintenanceHistoryFilter.empty();
        private int historyReads;
        private boolean addCalled;
        private MaintenanceFormHistoryContext formContext =
                MaintenanceFormHistoryContext.empty();
        private String lastFormContextCustomer;

        @Override
        public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByCustomer(
                String customerName, int page, int pageSize) {
            return getMaintenanceRecordsByCustomer(
                    customerName,
                    page,
                    pageSize,
                    MaintenanceHistoryFilter.empty());
        }

        @Override
        public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByCustomer(
                String customerName,
                int page,
                int pageSize,
                MaintenanceHistoryFilter filter) {
            historyReads++;
            lastHistoryCustomer = customerName;
            lastHistoryPage = page;
            lastHistoryPageSize = pageSize;
            lastHistoryFilter = filter;
            return historyPage;
        }

        @Override
        public MaintenanceRecordDTO getMaintenanceRecordById(
                Long maintenanceId) {
            recordReads++;
            return record;
        }

        @Override
        public MaintenanceFormHistoryContext getMaintenanceFormHistoryContext(
                String customerName,
                Date inspectionDate,
                Long excludedMaintenanceId) {
            lastFormContextCustomer = customerName;
            return formContext;
        }

        @Override
        public boolean addMaintenanceRecord(MaintenanceRecordDTO record) {
            addCalled = true;
            return true;
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
        private final CustomerDTO customer = customer();
        private int customerReads;
        private int allCustomerReads;

        @Override
        public CustomerDTO getCustomerByName(String customerName) {
            customerReads++;
            return "Acme".equals(customerName) ? customer : null;
        }

        @Override
        public List<CustomerDTO> getAllCustomers(
                String sortField, String sortDirection, String filter) {
            allCustomerReads++;
            return List.of(customer);
        }

        private static CustomerDTO customer() {
            CustomerDTO customer = new CustomerDTO();
            customer.setCustomerName("Acme");
            customer.setManagerName("Alice");
            customer.setSubManagerName("Bob");
            customer.setVerticaVersion("23.4.0-13");
            customer.setLicenseSize("25TB");
            customer.setCustomerType("정기점검 계약 고객사");
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

}
