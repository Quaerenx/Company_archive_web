package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.company.model.CustomerAssignmentDAO;
import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.MaintenanceCustomerAssignment;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.MaintenanceSchedule;
import com.company.model.UserDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaintenancePersonalCardsQueryTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-31T15:00:00Z"), ZoneOffset.UTC);

    @Test
    void personalCardsUseSessionIdAndShareOneDeduplicatedMonthQuery()
            throws Exception {
        CustomerDTO primary = customer("my-primary", "Same Name", null);
        CustomerDTO secondary = customer("my-sub-assignment", "Other Manager", "Same Name");
        CustomerDTO noPrimary = customer("my-sub-without-primary", null, "Same Name");
        CustomerDTO anotherUsers = customer("same-name-other-users-customer", "Same Name", null);
        StubCustomerDAO customers = new StubCustomerDAO();
        customers.customers = List.of(primary, secondary, noPrimary, anotherUsers);
        StubAssignmentDAO assignments = new StubAssignmentDAO();
        assignments.personalCustomers = List.of(primary, secondary, noPrimary);
        assignments.allAssignments = customers.customers.stream()
                .map(customer -> new MaintenanceCustomerAssignment(
                        customer.getCustomerName(), customer.getManagerName()))
                .toList();
        StubRecordDAO records = new StubRecordDAO();
        records.records = List.of(record("my-primary"), record("my-sub-without-primary"));
        RequestFixture request = new RequestFixture();
        request.parameters.put("userId", "another-user");
        request.parameters.put("userName", "Other Manager");

        new MaintenanceServlet(records, customers, assignments, FIXED_CLOCK)
                .doGet(request.proxy(), response());

        assertEquals("session-owner", assignments.lastUserId);
        assertEquals("Same Name", assignments.lastDisplayName);
        assertEquals(1, assignments.personalReads);
        assertEquals(1, assignments.allAssignmentReads);
        assertSame(assignments.personalCustomers,
                request.attributes.get("personalMaintenanceCustomers"));
        Map<?, ?> global = (Map<?, ?>) request.attributes.get("inspectorCustomers");
        assertEquals(List.of(anotherUsers), global.get("Same Name"));
        assertEquals(null, global.get("Other Manager"));
        assertEquals(1, global.size());
        assertEquals(1, records.monthReads);
        assertEquals(4, records.customerNames.size());
        assertEquals(new HashSet<>(List.of(
                "my-primary", "my-sub-assignment", "my-sub-without-primary",
                "same-name-other-users-customer")), new HashSet<>(records.customerNames));
        assertEquals(Date.valueOf("2026-09-01"), records.monthStart);
        assertEquals(Date.valueOf("2026-10-01"), records.monthEnd);
        assertEquals(Map.of("my-primary", true, "my-sub-without-primary", true),
                request.attributes.get("currentMonthMaintenanceCustomers"));
        assertEquals("/maintenance/maintenance_cards.jsp", request.forwardedPath);
        assertEquals(1, customers.allReads);
        assertEquals("manager_name", customers.sortField);
        assertEquals("ASC", customers.sortDirection);
        assertEquals("maintenance", customers.filter);
        assertEquals("2026-09", request.attributes.get("maintenanceMonthParam"));
        assertEquals("all", request.attributes.get("registrationStatus"));
        assertEquals(3, request.attributes.get("personalMaintenanceAssignedCount"));
        assertEquals(1, request.attributes.get("globalMaintenanceCustomerCount"));
    }

    @Test
    void personalOnlyCustomersRemoveEveryGlobalGroupButKeepMonthStatus()
            throws Exception {
        CustomerDTO primary = customer("my-primary", "Same Name", null);
        CustomerDTO secondary = customer("my-sub-assignment", "Other Manager", "Same Name");
        StubCustomerDAO customers = new StubCustomerDAO();
        customers.customers = List.of(primary, secondary);
        StubAssignmentDAO assignments = new StubAssignmentDAO();
        assignments.personalCustomers = customers.customers;
        assignments.allAssignments = List.of(
                new MaintenanceCustomerAssignment("my-primary", "Same Name"),
                new MaintenanceCustomerAssignment("my-sub-assignment", "Other Manager"));
        StubRecordDAO records = new StubRecordDAO();
        records.records = List.of(record("my-sub-assignment"));
        RequestFixture request = new RequestFixture();

        new MaintenanceServlet(records, customers, assignments, FIXED_CLOCK)
                .doGet(request.proxy(), response());

        assertSame(assignments.personalCustomers,
                request.attributes.get("personalMaintenanceCustomers"));
        assertEquals(Map.of(), request.attributes.get("inspectorCustomers"));
        assertEquals(List.of("my-primary", "my-sub-assignment"), records.customerNames);
        assertEquals(1, records.monthReads);
        assertEquals(Map.of("my-sub-assignment", true),
                request.attributes.get("currentMonthMaintenanceCustomers"));
    }

    @Test
    void overlapMatchesTrimmedCustomerNamesWithoutMergingDifferentNames()
            throws Exception {
        CustomerDTO globalOverlap = customer("Acme", "Other Manager", "Same Name");
        CustomerDTO distinct = customer("ACME", "Other Manager", null);
        StubCustomerDAO customers = new StubCustomerDAO();
        customers.customers = List.of(globalOverlap, distinct);
        StubAssignmentDAO assignments = new StubAssignmentDAO();
        assignments.personalCustomers = List.of(customer(" Acme ", "Other Manager", "Same Name"));
        RequestFixture request = new RequestFixture();

        new MaintenanceServlet(new StubRecordDAO(), customers, assignments, FIXED_CLOCK)
                .doGet(request.proxy(), response());

        assertEquals(Map.of("Other Manager", List.of(distinct)),
                request.attributes.get("inspectorCustomers"));
        assertSame(assignments.personalCustomers,
                request.attributes.get("personalMaintenanceCustomers"));
    }

    @Test
    void emptyPersonalCardsDoNotUseSameNameGlobalGroupAsAssignments()
            throws Exception {
        CustomerDTO anotherUsers = customer("other-users-customer", "Same Name", null);
        StubCustomerDAO customers = new StubCustomerDAO();
        customers.customers = List.of(anotherUsers);
        StubAssignmentDAO assignments = new StubAssignmentDAO();
        assignments.allAssignments = List.of(new MaintenanceCustomerAssignment(
                anotherUsers.getCustomerName(), anotherUsers.getManagerName()));
        StubRecordDAO records = new StubRecordDAO();
        RequestFixture request = new RequestFixture();

        new MaintenanceServlet(records, customers, assignments, FIXED_CLOCK)
                .doGet(request.proxy(), response());

        assertEquals(List.of(), request.attributes.get("personalMaintenanceCustomers"));
        assertEquals(Map.of("Same Name", List.of(anotherUsers)),
                request.attributes.get("inspectorCustomers"));
        assertEquals(List.of("other-users-customer"), records.customerNames);
        assertEquals(1, records.monthReads);
        assertEquals(1, assignments.personalReads);
    }

    @Test
    void noCustomersSkipsMonthRecordQueryAndExposesEmptySections()
            throws Exception {
        StubRecordDAO records = new StubRecordDAO();
        StubAssignmentDAO assignments = new StubAssignmentDAO();
        RequestFixture request = new RequestFixture();

        new MaintenanceServlet(records, new StubCustomerDAO(), assignments, FIXED_CLOCK)
                .doGet(request.proxy(), response());

        assertEquals(List.of(), request.attributes.get("personalMaintenanceCustomers"));
        assertEquals(Map.of(), request.attributes.get("inspectorCustomers"));
        assertEquals(Map.of(), request.attributes.get("currentMonthMaintenanceCustomers"));
        assertEquals(0, records.monthReads);
        assertEquals(1, assignments.personalReads);
    }

    @Test
    void selectedMonthRegisteredFilterIncludesFutureAndOffCycleRecordsInOneBatch()
            throws Exception {
        CustomerDTO future = customer("future-record", "Same Name", null);
        CustomerDTO offCycle = customer("off-cycle-record", "Other Manager", "Same Name");
        CustomerDTO noRecord = customer("no-record", "Same Name", null);
        StubAssignmentDAO assignments = new StubAssignmentDAO();
        assignments.personalCustomers = List.of(future, offCycle, noRecord);
        assignments.allAssignments = List.of(
                new MaintenanceCustomerAssignment("future-record", "Same Name"),
                new MaintenanceCustomerAssignment("off-cycle-record", "Other Manager", quarterly("2026-09")),
                new MaintenanceCustomerAssignment("no-record", "Same Name"));
        StubRecordDAO records = new StubRecordDAO();
        MaintenanceRecordDTO futureRecord = record("future-record");
        futureRecord.setInspectionDate(Date.valueOf("2026-10-20"));
        MaintenanceRecordDTO extraRecord = record("off-cycle-record");
        extraRecord.setInspectionDate(Date.valueOf("2026-10-05"));
        records.records = List.of(futureRecord, extraRecord);
        RequestFixture request = new RequestFixture();
        request.parameters.put("maintenanceMonth", "2026-10");
        request.parameters.put("registrationStatus", "registered");

        new MaintenanceServlet(records, new StubCustomerDAO(), assignments, FIXED_CLOCK)
                .doGet(request.proxy(), response());

        assertEquals(List.of(future, offCycle), request.attributes.get("personalMaintenanceCustomers"));
        assertEquals("2026-10", request.attributes.get("maintenanceMonthParam"));
        assertEquals("registered", request.attributes.get("registrationStatus"));
        assertEquals(Date.valueOf("2026-10-01"), records.monthStart);
        assertEquals(Date.valueOf("2026-11-01"), records.monthEnd);
        assertEquals(1, records.monthReads);
        assertEquals(1, assignments.allAssignmentReads);
        assertEquals(3, records.customerNames.size());
        assertEquals(Map.of("future-record", true, "off-cycle-record", false, "no-record", true),
                request.attributes.get("maintenanceDueCustomers"));
        assertEquals(3, request.attributes.get("personalMaintenanceAssignedCount"));
    }

    @Test
    void unregisteredFilterShowsOnlyDueCustomersAndKeepsUnfilteredCounts()
            throws Exception {
        CustomerDTO monthly = customer("monthly", "Same Name", null);
        CustomerDTO quarterlyDue = customer("quarterly-due", "Other Manager", "Same Name");
        CustomerDTO offCycle = customer("off-cycle", "Same Name", null);
        CustomerDTO registered = customer("registered", "Same Name", null);
        CustomerDTO disabled = customer("disabled", "Same Name", null);
        CustomerDTO global = customer("other-customer", "Same Name", null);
        StubAssignmentDAO assignments = new StubAssignmentDAO();
        assignments.personalCustomers = List.of(monthly, quarterlyDue, offCycle, registered, disabled);
        assignments.allAssignments = List.of(
                new MaintenanceCustomerAssignment("monthly", "Same Name"),
                new MaintenanceCustomerAssignment("quarterly-due", "Other Manager", quarterly("2026-09")),
                new MaintenanceCustomerAssignment("off-cycle", "Same Name", quarterly("2026-08")),
                new MaintenanceCustomerAssignment("registered", "Same Name"),
                new MaintenanceCustomerAssignment("disabled", "Same Name", new MaintenanceSchedule(
                        1, YearMonth.of(2000, 1), LocalDate.of(2000, 1, 1), null, false)),
                new MaintenanceCustomerAssignment("other-customer", "Same Name"));
        StubCustomerDAO customers = new StubCustomerDAO();
        customers.customers = List.of(monthly, quarterlyDue, offCycle, registered, disabled, global);
        StubRecordDAO records = new StubRecordDAO();
        records.records = List.of(record("registered"), record("other-customer"));
        RequestFixture request = new RequestFixture();
        request.parameters.put("registrationStatus", "unregistered");

        new MaintenanceServlet(records, customers, assignments, FIXED_CLOCK)
                .doGet(request.proxy(), response());

        assertEquals(List.of(monthly, quarterlyDue), request.attributes.get("personalMaintenanceCustomers"));
        assertEquals(Map.of(), request.attributes.get("inspectorCustomers"));
        assertEquals(5, request.attributes.get("personalMaintenanceAssignedCount"));
        assertEquals(1, request.attributes.get("globalMaintenanceCustomerCount"));
        assertEquals(1, records.monthReads);
        assertEquals(6, records.customerNames.size());
        assertEquals(false, ((Map<?, ?>) request.attributes.get("maintenanceDueCustomers")).get("off-cycle"));
        assertEquals(false, ((Map<?, ?>) request.attributes.get("maintenanceDueCustomers")).get("disabled"));
    }

    @Test
    void invalidMonthAndStatusReturnBadRequestBeforeAnyDaoReads() throws Exception {
        for (Map<String, String> parameters : List.of(
                Map.of("maintenanceMonth", "2026-13"),
                Map.of("maintenanceMonth", "2026-9"),
                Map.of("maintenanceMonth", "2026-09-01"),
                Map.of("maintenanceMonth", "1899-12"),
                Map.of("maintenanceMonth", "2101-01"),
                Map.of("registrationStatus", "pending"),
                Map.of("registrationStatus", "ALL"))) {
            StubRecordDAO records = new StubRecordDAO();
            StubCustomerDAO customers = new StubCustomerDAO();
            StubAssignmentDAO assignments = new StubAssignmentDAO();
            RequestFixture request = new RequestFixture();
            request.parameters.putAll(parameters);
            int[] status = {200};
            HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(), new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> {
                        if ("sendError".equals(call.getName()) || "setStatus".equals(call.getName())) {
                            status[0] = (Integer) args[0];
                        }
                        return defaultValue(call.getReturnType());
                    });

            new MaintenanceServlet(records, customers, assignments, FIXED_CLOCK)
                    .doGet(request.proxy(), response);

            assertEquals(400, status[0], parameters.toString());
            assertEquals(null, request.forwardedPath);
            assertEquals(0, records.monthReads);
            assertEquals(0, customers.allReads);
            assertEquals(0, assignments.personalReads);
            assertEquals(0, assignments.allAssignmentReads);
        }
    }

    @Test
    void supportedMonthBoundariesKeepTheNextMonthExclusiveAndBlankStatusDefaultsToAll()
            throws Exception {
        for (String month : List.of("1900-01", "2100-12")) {
            CustomerDTO customer = customer("assigned", "Same Name", null);
            StubAssignmentDAO assignments = new StubAssignmentDAO();
            assignments.personalCustomers = List.of(customer);
            assignments.allAssignments = List.of(new MaintenanceCustomerAssignment("assigned", "Same Name"));
            StubRecordDAO records = new StubRecordDAO();
            RequestFixture request = new RequestFixture();
            request.parameters.put("maintenanceMonth", " " + month + " ");
            request.parameters.put("registrationStatus", " ");

            new MaintenanceServlet(records, new StubCustomerDAO(), assignments, FIXED_CLOCK)
                    .doGet(request.proxy(), response());

            YearMonth selected = YearMonth.parse(month);
            assertEquals(month, request.attributes.get("maintenanceMonthParam"));
            assertEquals("all", request.attributes.get("registrationStatus"));
            assertEquals(Date.valueOf(selected.atDay(1)), records.monthStart);
            assertEquals(Date.valueOf(selected.plusMonths(1).atDay(1)), records.monthEnd);
            assertEquals(1, records.monthReads);
            assertEquals(List.of(customer), request.attributes.get("personalMaintenanceCustomers"));
        }
    }

    private static MaintenanceSchedule quarterly(String anchor) {
        return new MaintenanceSchedule(3, YearMonth.parse(anchor), LocalDate.of(2000, 1, 1), null, true);
    }

    private static CustomerDTO customer(String name, String primary, String secondary) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(name);
        customer.setManagerName(primary);
        customer.setSubManagerName(secondary);
        customer.setCustomerType("정기점검 계약 고객사");
        return customer;
    }

    private static MaintenanceRecordDTO record(String customerName) {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setCustomerName(customerName);
        record.setInspectionDate(Date.valueOf("2026-09-01"));
        return record;
    }

    private static final class StubCustomerDAO extends CustomerDAO {
        private List<CustomerDTO> customers = List.of();
        private int allReads;
        private String sortField;
        private String sortDirection;
        private String filter;

        @Override
        public List<CustomerDTO> getAllCustomers(String sortField, String sortDirection, String filter) {
            allReads++;
            this.sortField = sortField;
            this.sortDirection = sortDirection;
            this.filter = filter;
            return customers;
        }
    }

    private static final class StubAssignmentDAO extends CustomerAssignmentDAO {
        private List<CustomerDTO> personalCustomers = List.of();
        private List<MaintenanceCustomerAssignment> allAssignments = List.of();
        private int personalReads;
        private int allAssignmentReads;
        private String lastUserId;
        private String lastDisplayName;

        @Override
        public List<CustomerDTO> getMaintenanceCustomersByAssignee(String userId, String displayName) {
            personalReads++;
            lastUserId = userId;
            lastDisplayName = displayName;
            return personalCustomers;
        }

        @Override
        public List<MaintenanceCustomerAssignment> getAllMaintenanceCustomerAssignments() {
            allAssignmentReads++;
            return allAssignments;
        }
    }

    private static final class StubRecordDAO extends MaintenanceRecordDAO {
        private List<MaintenanceRecordDTO> records = List.of();
        private int monthReads;
        private Date monthStart;
        private Date monthEnd;
        private List<String> customerNames;

        @Override
        public List<MaintenanceRecordDTO> getMaintenanceRecordsByMonthForCustomers(
                Date monthStart, Date monthEnd, List<String> customerNames) {
            monthReads++;
            this.monthStart = monthStart;
            this.monthEnd = monthEnd;
            this.customerNames = List.copyOf(customerNames);
            return records;
        }
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private String forwardedPath;

        private HttpServletRequest proxy() {
            UserDTO user = new UserDTO("session-owner", "", "Same Name", "QA");
            HttpSession session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(), new Class<?>[] {HttpSession.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getAttribute" -> "user".equals(args[0]) ? user : null;
                        default -> defaultValue(call.getReturnType());
                    });
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(), new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getSession" -> session;
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "getAttribute" -> attributes.get((String) args[0]);
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
                    RequestDispatcher.class.getClassLoader(), new Class<?>[] {RequestDispatcher.class},
                    (ignored, call, args) -> {
                        if ("forward".equals(call.getName())) {
                            forwardedPath = path;
                        }
                        return defaultValue(call.getReturnType());
                    });
        }
    }

    private static HttpServletResponse response() {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(), new Class<?>[] {HttpServletResponse.class},
                (ignored, call, args) -> defaultValue(call.getReturnType()));
    }
}
