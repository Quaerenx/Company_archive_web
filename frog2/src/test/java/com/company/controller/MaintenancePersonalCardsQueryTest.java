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
import com.company.model.UserDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
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
        assertEquals(List.of(primary, anotherUsers), global.get("Same Name"));
        assertEquals(List.of(secondary), global.get("Other Manager"));
        assertEquals(2, global.size());
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
